package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;
import static junit.framework.TestCase.*;

import java.util.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.Activation.*;
import com.obsidiandynamics.indigo.benchmark.Summary.*;

/**
 *  Benchmarks request-response pair throughput.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class RequestResponseBenchmark implements TestSupport, BenchmarkSupport {
  abstract static class Config implements Spec {
    int threads;
    int actors;
    int bias;
    int pairs;
    int seedPairs;
    float warmupFrac;
    long timeout;
    LogConfig log;
    boolean stats;
    boolean statsSync;
    int statsSamples;
    
    /* Derived fields. */
    int warmupPairs;
    int statsPeriod;
    
    @Override public void init() {
      warmupPairs = (int) (pairs * warmupFrac);
      statsPeriod = Math.max(1, (pairs - warmupPairs) * actors / statsSamples);
    }

    @Override public LogConfig getLog() {
      return log;
    }

    @Override public String describe() {
      return String.format("%d threads, %,d send actors, %,d pairs/actor, %,d seed pairs/actor, %.0f%% warmup fraction", 
                           threads, actors, pairs, seedPairs, warmupFrac * 100);
    }

    @Override public Summary run() {
      return new RequestResponseBenchmark().test(this);
    }
  }
  
  private static final class DriverState implements Elapsed {
    final ActorRef to;
    
    int rx;
    int tx;
    
    int txOnStart;
    long totalProcessed;
    long started;
    long timeTaken;
    
    DriverState(Activation a) {
      to = ActorRef.of(ECHO, a.self().key());
    }
    
    @Override public long getTotalProcessed() { return totalProcessed; }
    @Override public long getTimeTaken() { return timeTaken; }
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
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
      statsSamples = 1_000;
    }}.test();
  }
  
  private Summary test(Config c) {
    if (c.seedPairs > c.pairs) 
      throw new IllegalArgumentException("Seed pairs cannot be greater than total number of pairs");

    final LogConfig log = c.log;
    final Set<DriverState> states = new HashSet<>();
    final Summary summary = new Summary();
    
    if (log.stages) log.out.format("Warming up...\n");
    
    new TestActorSystemConfig() {{
      parallelism = c.threads;
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        bias = c.bias;
        backlogThrottleCapacity = Integer.MAX_VALUE;
      }};
    }}
    .define()
    .when(DRIVER).lambda(DriverState::new, (a, m, s) -> {
      send(a, s.to, s, c, c.seedPairs, summary.stats);
    })
    .when(ECHO).lambda((a, m) -> a.reply(m).tell())
    .when(TIMER).lambda((a, m) -> states.add(m.body()))
    .ingress().times(c.actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdown();

    assertEquals(c.actors, states.size());
    
    summary.compute(states, c.actors);
    return summary;
  }
  
  private static void send(Activation a, ActorRef to, DriverState s, Config c, int times, Stats stats) {
    final long startTime = c.stats ? System.nanoTime() : 0;
    final MessageBuilder m = a.to(to).times(times).ask();
    if (c.timeout != 0) {
      m.await(c.timeout).onTimeout(() -> fail("Timed out waiting for " + to));
    }
    m.onResponse(onResponse(a, to, s, c, startTime, stats));
    s.tx += times;
  }
  
  private static Consumer<Message> onResponse(Activation a, ActorRef to, DriverState s, Config c, long sendTime, Stats stats) {
    return m -> {
      final LogConfig log = c.log;
      if (c.stats && s.rx >= c.warmupPairs && (s.rx - c.warmupPairs) % c.statsPeriod == 0) {
        final long took = System.nanoTime() - sendTime;
        if (c.statsSync) {
          stats.samples.addValue(took);
        } else {
          a.<Long>egress(stats.samples::addValue).using(stats.executor).tell(took);
        }
      }
      
      s.rx++;
      if (log.verbose) log.out.format("%s received from %s (rx=%,d, tx=%,d)\n", a.self(), m.from(), s.rx, s.tx);
      
      if (s.rx == c.warmupPairs) {
        s.txOnStart = s.tx;
        if (log.stages && a.self().key().equals("0")) log.out.format("Starting timed run...\n");
        s.started = System.nanoTime();
      }
      
      if (s.rx == c.pairs) {
        if (log.verbose) log.out.format("Done %s\n", a.self());
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
      pairs = 20_000_000;
      seedPairs = 1_000;
      warmupFrac = .25f;
      timeout = 0;
      log = new LogConfig() {{
        summary = stages = true;
      }};
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.test();
  }
}

