package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorSystemConfig.Executor.*;
import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.commons.math3.stat.descriptive.*;
import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.Activation.*;

/**
 *  Benchmarks request-response pair throughput.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class RequestResponseBenchmark implements TestSupport {
  private static final class State {
    int rx;
    int tx;
    
    int txOnStart;
    int totalProcessed;
    long started;
    long took;
  }
  
  private static final class Timings {
    long timedTxns = 0;
    long avgTime = 0;
    final Stats stats = new Stats();
  }
  
  private static class Stats {
    final DescriptiveStatistics samples = new DescriptiveStatistics();
    final ExecutorService executor = Executors.newFixedThreadPool(1);
    
    void await() {
      executor.shutdown();
      try {
        executor.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {}
    }
  }
  
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String TIMER = "timer";
  
  @Test
  public void test() {
    final Config c = new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = 4;
      bias = 1_000;
      pairs = 1_000;
      seedPairs = 100;
      warmupFrac = .05f;
      log = LOG;
    }};
    test(c);
  }
  
  private static abstract class Config {
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
    
    /* Derived fields. */
    int warmupPairs;
    int statsPeriod;
    
    void init() {
      warmupPairs = (int) (pairs * warmupFrac);
      statsSamples = Math.max(1, (pairs - warmupPairs) * actors / statsSamples);
    }
  }
  
  private Timings test(Config c) {
    c.init();
    
    if (c.seedPairs > c.pairs) 
      throw new IllegalArgumentException("Seed pairs cannot be greater than total number of pairs");
    
    final Set<State> states = new HashSet<>();

    final Timings t = new Timings();
    
    if (c.log) System.out.format("Warming up...\n");
    
    new ActorSystemConfig() {{
      parallelism = c.threads;
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        priority = c.bias;
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
    
    for (State s : states) {
      t.timedTxns += s.totalProcessed;
      t.avgTime += s.took;
    }
    t.avgTime /= c.actors;
    t.stats.await();
    
    return t;
  }
  
  private static void send(Activation a, ActorRef to, State s, Config c, int times, Stats stats) {
    final long startTime = c.stats ? System.nanoTime() : 0;
    final MessageBuilder m = a.to(to).times(times).ask();
    if (c.timeout != 0) {
      m.await(c.timeout).onTimeout(t -> {
        fail("Timed out waiting for " + to);
      });
    }
    m.onResponse(onResponse(to, s, c, startTime, stats));
    s.tx += times;
  }
  
  private static Consumer<Activation> onResponse(ActorRef to, State s, Config c, long startTime, Stats stats) {
    return a -> {
      if (c.stats && s.rx >= c.warmupPairs && (s.rx - c.warmupPairs) % c.statsSamples == 0) {
        if (c.statsSync) {
          stats.samples.addValue(System.nanoTime() - startTime);
        } else {
          a.<Long>egress(stats.samples::addValue).using(stats.executor).tell(System.nanoTime() - startTime);
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
        s.took = (System.nanoTime() - s.started) / 1_000_000l;
        s.totalProcessed = (c.pairs - c.warmupPairs + c.pairs - s.txOnStart) / 2;
        a.to(ActorRef.of(TIMER)).tell(s);
      } else if (s.tx != c.pairs) {
        send(a, to, s, c, 1, stats);
      }
    };
  }
  
  public static void main(String[] args) {
    final Config c = new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 2;
      bias = 1;
      pairs = 1_000_000;
      seedPairs = 1;
      warmupFrac = .05f;
      timeout = 0;
      log = true;
      verbose = false;
      stats = true;
      statsSync = true;
      statsSamples = 1_000;
    }};
    System.out.format("Request-response pairs benchmark...\n");
    System.out.format("%d threads, %,d send actors, %,d pairs/actor, %,d seed pairs/actor, %.0f%% warmup fraction\n", 
                      c.threads, c.actors, c.pairs, c.seedPairs, c.warmupFrac * 100);

    final Timings t = new RequestResponseBenchmark().test(c);
    System.out.format("%,d txns took %,d s, %,d txn/s\n", t.timedTxns, t.avgTime / 1000, t.timedTxns / Math.max(1, t.avgTime) * 1000);
    if (c.stats) {
      System.out.format("Latency: mean: %,.1f, sd: %,.1f, min: %,.1f, 50%%: %,.1f, 95%%: %,.1f, 99%%: %,.1f, max: %,.1f (µs, N=%,d)\n", 
                        t.stats.samples.getMean() / 1000, 
                        t.stats.samples.getStandardDeviation() / 1000,
                        t.stats.samples.getMin() / 1000,
                        t.stats.samples.getPercentile(5) / 1000, 
                        t.stats.samples.getPercentile(95) / 1000,
                        t.stats.samples.getPercentile(99) / 1000,
                        t.stats.samples.getMax() / 1000,
                        t.stats.samples.getN());
    }
  }
}

