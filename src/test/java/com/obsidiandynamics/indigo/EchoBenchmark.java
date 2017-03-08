package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.Executor.FIXED_THREAD_POOL;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

/**
 *  Benchmarks raw message throughput. Nearly identical to 
 *  http://letitcrash.com/post/20397701710/50-million-messages-per-second-on-a-single.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class EchoBenchmark implements TestSupport {
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
    test(4, 1_000, 100, 0f, false, false);
  }
  
  private Timings test(int actors, int messages, int seedMessages, float warmupFrac, boolean log, boolean verbose) {
    if (seedMessages > messages / 2) 
      throw new IllegalArgumentException("Seed messages cannot be greater than half the total number of messages");
    
    final int warmupMessages = (int) (messages * warmupFrac);
    final Set<State> states = new HashSet<>();
    
    if (log) System.out.format("Warming up...\n");
    
    new ActorSystemConfig() {{
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        priority = 1_000;
      }};
    }}
    .define()
    .when(DRIVER).lambda(State::new, (a, s) -> {
      switch (a.message().from().role()) {
        case ECHO:
          s.rx++;
          if (verbose) System.out.format("%s received from %s (rx=%,d, tx=%,d)\n", a.self(), a.message().from(), s.rx, s.tx);
          
          if (s.rx == warmupMessages) {
            s.txOnStart = s.tx;
            if (log && a.self().key().equals("0")) System.out.format("Starting timed run...\n");
            s.started = System.nanoTime();
          }
          
          if (s.rx == messages / 2 && s.tx == messages / 2) {
            if (verbose) System.out.format("Done %s\n", a.self());
            s.took = (System.nanoTime() - s.started) / 1_000_000l;
            s.totalProcessed = messages / 2 - warmupMessages + messages / 2 - s.txOnStart;
            a.to(ActorRef.of(TIMER)).tell(s);
          } else if (s.tx != messages / 2) {
            s.tx++;
            a.reply();
          }
          break;
          
        case INGRESS:
          a.to(ActorRef.of(ECHO, a.message().body().toString())).times(seedMessages).tell();
          s.tx += seedMessages;
          break;
          
        default: throw new UnsupportedOperationException(a.message().from().role());
      }
    })
    .when(ECHO).lambda(a -> a.reply())
    .when(TIMER).lambda(a -> states.add(a.message().body()))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(i))
    .shutdown();

    assertEquals(actors, states.size());
    
    final Timings t = new Timings();
    for (State s : states) {
      t.timedMessages += s.totalProcessed;
      t.avgTime += s.took;
    }
    t.avgTime /= actors;
    
    return t;
  }
  
  public static void main(String[] args) {
    final int threads = Runtime.getRuntime().availableProcessors();
    final int actors = threads * 2;
    final int messages = 100_000_000;
    final int seedMessages = 1_000;
    final float warmupFrac = .05f;
    System.out.format("Running benchmark...\n");
    System.out.format("%d threads, %,d send actors, %,d messages/actor, %,d seed messages/actor, %.0f%% warmup fraction\n", 
                      threads, actors, messages, seedMessages, warmupFrac * 100);
    final Timings t = new EchoBenchmark().test(actors, messages, seedMessages, warmupFrac, true, false);
    System.out.format("Took %,d s, %,d msgs/s\n", t.avgTime / 1000, t.timedMessages / t.avgTime * 1000);
  }
}

