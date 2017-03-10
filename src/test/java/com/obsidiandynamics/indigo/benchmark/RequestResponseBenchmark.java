package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorSystemConfig.Executor.*;
import static junit.framework.TestCase.*;

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
    long timedMessages = 0;
    long avgTime = 0;
  }
  
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String TIMER = "timer";
  
  @Test
  public void test() {
    final Config c = new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = 4;
      pairs = 1_000;
      seedPairs = 100;
      warmupPairs = 0;
      log = LOG;
    }};
    test(c);
  }
  
  private static abstract class Config {
    int threads;
    int actors; 
    int pairs; 
    int seedPairs;
    int warmupPairs;
    long timeout = 1_000;
    boolean log; 
    boolean verbose;
  }
  
  private Timings test(Config c) {
    if (c.seedPairs > c.pairs) 
      throw new IllegalArgumentException("Seed pairs cannot be greater than total number of pairs");
    
    final Set<State> states = new HashSet<>();
    
    if (c.log) System.out.format("Warming up...\n");
    
    new ActorSystemConfig() {{
      parallelism = c.threads;
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        priority = 1_000;
      }};
    }}
    .define()
    .when(DRIVER).lambda(State::new, (a, s) -> {
      final ActorRef to = ActorRef.of(ECHO, a.message().body().toString());
      send(a, to, s, c, c.seedPairs);
    })
    .when(ECHO).lambda(a -> a.reply())
    .when(TIMER).lambda(a -> states.add(a.message().body()))
    .ingress().times(c.actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(i))
    .shutdown();

    assertEquals(c.actors, states.size());
    
    final Timings t = new Timings();
    for (State s : states) {
      t.timedMessages += s.totalProcessed;
      t.avgTime += s.took;
    }
    t.avgTime /= c.actors;
    
    return t;
  }
  
  private static void send(Activation a, ActorRef to, State s, Config c, int times) {
    final MessageBuilder m = a.to(to).times(times).ask();
    if (c.timeout != 0) {
      m.await(c.timeout).onTimeout(t -> {
        fail("Timed out waiting for " + to);
      });
    }
    m.onResponse(onResponse(to, s, c));
    s.tx += times;
  }
  
  private static Consumer<Activation> onResponse(ActorRef to, State s, Config c) {
    return a -> {
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
        send(a, to, s, c, 1);
      }
    };
  }
  
  public static void main(String[] args) {
    final float warmupFrac = .05f;
    final Config c = new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 2;
      pairs = 10_000_000;
      seedPairs = 1_000;
      warmupPairs = (int) (pairs * warmupFrac);
      timeout = 1_000;
      log = true;
      verbose = false;
    }};
    System.out.format("Running benchmark...\n");
    System.out.format("%d threads, %,d send actors, %,d pairs/actor, %,d seed pairs/actor, %.0f%% warmup fraction\n", 
                      c.threads, c.actors, c.pairs, c.seedPairs, warmupFrac * 100);

    final Timings t = new RequestResponseBenchmark().test(c);
    System.out.format("Took %,d s, %,d pair/s\n", t.avgTime / 1000, t.timedMessages / Math.max(1, t.avgTime) * 1000);
  }
}

