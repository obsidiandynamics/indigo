package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorRef.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;
import static junit.framework.TestCase.*;

import java.io.*;
import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

/**
 *  Benchmarks raw message throughput. Nearly identical to 
 *  http://letitcrash.com/post/20397701710/50-million-messages-per-second-on-a-single.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class EchoBenchmark implements TestSupport, BenchmarkSupport {
  static abstract class Config {
    int threads;
    int actors;
    int bias;
    int messages;
    int seedMessages;
    float warmupFrac;
    boolean log;
    boolean verbose;
    boolean stats;
    boolean statsSync;
    int statsSamples;
    PrintStream out = System.out;
    
    /* Derived fields. */
    int warmupMessages;
    int statsPeriod;
    
    private void init() {
      warmupMessages = (int) (messages * warmupFrac) / 2;
      statsPeriod = Math.max(1, (messages / 2 - warmupMessages) * actors / statsSamples);
    }
    
    Timings test() {
      init();
      if (log) {
        out.format("Message echo benchmark...\n");
        out.format("%d threads, %,d send actors, %,d messages/actor, %,d seed messages/actor, %.0f%% warmup fraction\n", 
                   threads, actors, messages, seedMessages, warmupFrac * 100);
      }
      final Timings t = new EchoBenchmark().test(this);
      if (log) out.format("%s\n", t);
      return t;
    }
  }
  
  private static final class State implements TimedState {
    int rx;
    int tx;
    
    int txOnStart;
    int totalProcessed;
    long started;
    long timeTaken;
    
    @Override
    public long getTotalProcessed() { return totalProcessed; }
    @Override
    public long getTimeTaken() { return timeTaken; }
    
    long getSendTime(Config c) {
      if (c.stats && tx >= c.warmupMessages && (tx - c.warmupMessages) % c.statsPeriod == 0) {
        return System.nanoTime();
      } else {
        return 0;
      }
    }
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
      messages = 1_000;
      seedMessages = 100;
      warmupFrac = .05f;
      log = LOG;
      statsSamples = 1_000;
    }}.test();
  }
  
  private Timings test(Config c) {
    if (c.seedMessages > c.messages / 2) 
      throw new IllegalArgumentException("Seed messages cannot be greater than half the total number of messages");
    
    final Set<State> states = new HashSet<>();
    final Timings t = new Timings();
    
    if (c.log) System.out.format("Warming up...\n");
    
    new ActorSystemConfig() {{
      parallelism = c.threads;
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        bias = c.bias;
        backlogThrottleCapacity = Integer.MAX_VALUE;
      }};
    }}
    .define()
    .when(DRIVER).lambda(State::new, (a, s) -> {
      switch (a.message().from().role()) {
        case ECHO:
          s.rx++;
          if (c.verbose) System.out.format("%s received from %s (rx=%,d, tx=%,d)\n", a.self(), a.message().from(), s.rx, s.tx);
          
          if (s.rx == c.warmupMessages) {
            s.txOnStart = s.tx;
            if (c.log && a.self().key().equals("0")) System.out.format("Starting timed run...\n");
            s.started = System.nanoTime();
          }
          
          if (s.rx == c.messages / 2 && s.tx == c.messages / 2) {
            if (c.verbose) System.out.format("Done %s\n", a.self());
            s.timeTaken = (System.nanoTime() - s.started) / 1_000_000l;
            s.totalProcessed = c.messages / 2 - c.warmupMessages + c.messages / 2 - s.txOnStart;
            a.to(ActorRef.of(TIMER)).tell(s);
          } else if (s.tx != c.messages / 2) {
            a.reply(s.getSendTime(c));
            s.tx++;
          }
          break;
          
        case INGRESS:
          a.to(ActorRef.of(ECHO, a.message().body().toString())).times(c.seedMessages).tell(s.getSendTime(c));
          s.tx += c.seedMessages;
          break;
          
        default: throw new UnsupportedOperationException(a.message().from().role());
      }
    })
    .when(ECHO).lambda(a -> {
      final long sendTime = a.message().body();
      if (sendTime != 0) {
        final long took = System.nanoTime() - sendTime;
        if (c.statsSync) {
          t.stats.samples.addValue(took);
        } else {
          a.<Long>egress(t.stats.samples::addValue).using(t.stats.executor).tell(took);
        }
      }
      a.reply();
    })
    .when(TIMER).lambda(a -> states.add(a.message().body()))
    .ingress().times(c.actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(i))
    .shutdown();

    assertEquals(c.actors, states.size());
    
    t.compute(states, c.actors);
    return t;
  }
  
  public static void main(String[] args) {
    new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 4;
      bias = 1_000;
      messages = 50_000_000;
      seedMessages = 1_000;
      warmupFrac = .05f;
      log = true;
      verbose = false;
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.test();
  }
}

