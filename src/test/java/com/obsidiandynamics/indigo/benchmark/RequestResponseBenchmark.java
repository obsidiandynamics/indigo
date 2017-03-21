package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;
import static junit.framework.TestCase.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.Activation.*;

/**
 *  Benchmarks request-response pair throughput.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class RequestResponseBenchmark implements TestSupport, BenchmarkSupport {
  static abstract class Config {
    int threads;
    int actors;
    int bias;
    int pairs;
    int seedPairs;
    float warmupFrac;
    long timeout;
    boolean log;
    boolean verbose;
    boolean stats;
    boolean statsSync;
    int statsSamples;
    PrintStream out = System.out;
    
    /* Derived fields. */
    int warmupPairs;
    int statsPeriod;
    
    private void init() {
      warmupPairs = (int) (pairs * warmupFrac);
      statsPeriod = Math.max(1, (pairs - warmupPairs) * actors / statsSamples);
    }
    
    Timings test() {
      init();
      if (log) {
        out.format("Request-response pairs benchmark...\n");
        out.format("%d threads, %,d send actors, %,d pairs/actor, %,d seed pairs/actor, %.0f%% warmup fraction\n", 
                   threads, actors, pairs, seedPairs, warmupFrac * 100);
      }
      final Timings t = new RequestResponseBenchmark().test(this);
      if (log) out.format("%s\n", t);
      return t;
    }
  }
  
  private static final class State implements TimedState {
    int rx;
    int tx;
    
    int txOnStart;
    long totalProcessed;
    long started;
    long timeTaken;
    
    @Override
    public long getTotalProcessed() { return totalProcessed; }
    @Override
    public long getTimeTaken() { return timeTaken; }
  }
  
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String TIMER = "timer";
  
  @Test
  public void test() {
    new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = 4;
      bias = 1_000;
      pairs = 1_000;
      seedPairs = 100;
      warmupFrac = .05f;
      log = LOG;
      statsSamples = 1_000;
    }}.test();
  }
  
  private Timings test(Config c) {
    if (c.seedPairs > c.pairs) 
      throw new IllegalArgumentException("Seed pairs cannot be greater than total number of pairs");
    
    final Set<State> states = new HashSet<>();
    final Timings t = new Timings();
    
    if (c.log) System.out.format("Warming up...\n");
    
    new ActorSystemConfig() {{
      parallelism = c.threads;
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        bias = c.bias;
      }};
    }}
    .define()
    .when(DRIVER).lambda(State::new, (a, s) -> {
      final ActorRef to = ActorRef.of(ECHO, a.message().body().toString());
      send(a, to, s, c, c.seedPairs, t.stats);
    })
    .when(ECHO).lambda(a -> a.reply())
    .when(TIMER).lambda(a -> states.add(a.message().body()))
    .ingress().times(c.actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(i))
    .shutdown();

    assertEquals(c.actors, states.size());
    
    t.compute(states, c.actors);
    return t;
  }
  
  private static void send(Activation a, ActorRef to, State s, Config c, int times, Stats stats) {
    final long startTime = c.stats ? System.nanoTime() : 0;
    final MessageBuilder m = a.to(to).times(times).ask();
    if (c.timeout != 0) {
      m.await(c.timeout).onTimeout(t -> fail("Timed out waiting for " + to));
    }
    m.onResponse(onResponse(to, s, c, startTime, stats));
    s.tx += times;
  }
  
  private static Consumer<Activation> onResponse(ActorRef to, State s, Config c, long sendTime, Stats stats) {
    return a -> {
      if (c.stats && s.rx >= c.warmupPairs && (s.rx - c.warmupPairs) % c.statsPeriod == 0) {
        final long took = System.nanoTime() - sendTime;
        if (c.statsSync) {
          stats.samples.addValue(took);
        } else {
          a.<Long>egress(stats.samples::addValue).using(stats.executor).tell(took);
        }
      }
      
      s.rx++;
      if (c.verbose) System.out.format("%s received from %s (rx=%,d, tx=%,d)\n", a.self(), a.message().from(), s.rx, s.tx);
      
      if (s.rx == c.warmupPairs) {
        s.txOnStart = s.tx;
        if (c.log && a.self().key().equals("0")) System.out.format("Starting timed run...\n");
        s.started = System.nanoTime();
      }
      
      if (s.rx == c.pairs) {
        if (c.verbose) System.out.format("Done %s\n", a.self());
        s.timeTaken = (System.nanoTime() - s.started) / 1_000_000l;
        s.totalProcessed = (c.pairs - c.warmupPairs + c.pairs - s.txOnStart) / 2;
        a.to(ActorRef.of(TIMER)).tell(s);
      } else if (s.tx != c.pairs) {
        send(a, to, s, c, 1, stats);
      }
    };
  }
  
  public static void main(String[] args) {
    new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 2;
      bias = 1_000;
      pairs = 10_000_000;
      seedPairs = 1_000;
      warmupFrac = .05f;
      timeout = 0;
      log = true;
      verbose = false;
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.test();
  }
}

