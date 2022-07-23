package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.*;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.util.*;

/**
 *  Benchmarks raw message throughput. Nearly identical to 
 *  <a href="http://letitcrash.com/post/20397701710/50-million-messages-per-second-on-a-single">this</a>.
 *
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms2G -Xmx2G -Xss1M -XX:+UseParallelGC
 */
public final class EchoBenchmark implements TestSupport, BenchmarkSupport {
  static abstract class Config implements Spec {
    ExecutorChoice executorChoice = null;
    int threads;
    int actors;
    int bias;
    int messages;
    int seedMessages;
    float warmupFrac;
    LogConfig log;
    boolean stats;
    boolean statsSync;
    int statsSamples;
    
    /* Derived fields. */
    int warmupMessages;
    int statsPeriod;
    
    @Override public void init() {
      warmupMessages = (int) (messages * warmupFrac) / 2;
      statsPeriod = Math.max(1, (messages / 2 - warmupMessages) * actors / statsSamples);
    }
    
    @Override public LogConfig getLog() {
      return log;
    }
    
    @Override public String describe() {
      return String.format("%d threads, %,d send actors, %,d messages/actor, %,d seed messages/actor, %.0f%% warmup fraction", 
                           threads, actors, messages, seedMessages, warmupFrac * 100);
    }
    
    @Override public Summary run() {
      return new EchoBenchmark().test(this);
    }
  }
  
  private static final class DriverState implements Elapsed {
    final Message blank;
    
    int rx;
    int tx;
    
    int txOnStart;
    int totalProcessed;
    long started;
    long timeTaken;
    
    DriverState(Activation a) {
      blank = Message.builder().to(ActorRef.of(ECHO, a.self().key())).from(a.self()).body(0L).build();
    }
    
    @Override public long getTotalProcessed() { return totalProcessed; }
    @Override public long getTimeTaken() { return timeTaken; }
    
    long getSendTime(Config c) {
      if (c.stats && tx >= c.warmupMessages && (tx - c.warmupMessages) % c.statsPeriod == 0) {
        return System.nanoTime();
      } else {
        return 0;
      }
    }
  }
  
  private static final class EchoState {
    final Message blank;
    
    EchoState(Activation a) {
      blank = Message.builder().to(ActorRef.of(DRIVER, a.self().key())).from(a.self()).body(0L).build();
    }
  }
  
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String TIMER = "timer";
  
  @Test
  public void test() throws Exception {
    new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = 4;
      bias = 1_000;
      messages = 1_000;
      seedMessages = 100;
      warmupFrac = .05f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
      stats = true;
      statsSync = true;
      statsSamples = 100;
    }}.test();
  }
  
  private Summary test(Config c) {
    if (c.seedMessages > c.messages / 2) 
      throw new IllegalArgumentException("Seed messages cannot be greater than half the total number of messages");

    final LogConfig log = c.log;
    final Set<DriverState> states = new HashSet<>();
    final Summary summary = new Summary();
    
    if (log.stages) log.out.format("Warming up...\n");
    
    new TestActorSystemConfig() {{
      if (c.executorChoice != null) {
        executor = c.executorChoice;
      }
      parallelism = c.threads;
      reaperPeriodMillis = 0;
      defaultActorConfig = new ActorConfig() {{
        bias = c.bias;
        backlogThrottleCapacity = Integer.MAX_VALUE;
        backlogThrottleTries = 1;
      }};
    }}
    .createActorSystem()
    .addExecutor(summary.stats.executor).named("summary-stats")
    .on(DRIVER).cue(DriverState::new, (a, m, s) -> {
      switch (m.from().role()) {
        case ECHO:
          s.rx++;
          if (log.verbose) log.out.format("%s received from %s (rx=%,d, tx=%,d)\n", a.self(), m.from(), s.rx, s.tx);
          
          if (s.rx == c.warmupMessages) {
            s.txOnStart = s.tx;
            if (log.stages && a.self().key().equals("0")) log.out.format("Starting timed run...\n");
            s.started = System.nanoTime();
          }
          
          if (s.rx == c.messages / 2 && s.tx == c.messages / 2) {
            if (log.verbose) log.out.format("Done %s\n", a.self());
            s.timeTaken = (System.nanoTime() - s.started) / 1_000_000L;
            s.totalProcessed = c.messages / 2 - c.warmupMessages + c.messages / 2 - s.txOnStart;
            a.to(ActorRef.of(TIMER)).tell(s);
          } else if (s.tx != c.messages / 2) {
            final long sendTime = s.getSendTime(c);
            if (sendTime == 0) {
              a.send(s.blank);
            } else {
              a.toSenderOf(m).tell(sendTime);
            }
            s.tx++;
          }
          break;
          
        case INGRESS:
          a.to(ActorRef.of(ECHO, a.self().key())).times(c.seedMessages).tell(s.getSendTime(c));
          s.tx += c.seedMessages;
          break;
          
        default: throw new UnsupportedOperationException(m.from().role());
      }
    })
    .on(ECHO).cue(EchoState::new, (a, m, s) -> {
      final long sendTime = m.body();
      if (sendTime != 0) {
        final long took = System.nanoTime() - sendTime;
        if (c.statsSync) {
          summary.stats.samples.addValue(took);
        } else {
          a.<Long>egress(summary.stats.samples::addValue).withExecutor("summary-stats").tell(took);
        }
      }
      a.send(s.blank);
    })
    .on(TIMER).cue((a, m) -> states.add(m.body()))
    .ingress().times(c.actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdownSilently();

    assertEquals(c.actors, states.size());
    
    summary.compute(states);
    return summary;
  }
  
  public static void main(String[] args) throws Exception {
    new Config() {{
      executorChoice = ActorSystemConfig.ExecutorChoice.FIXED_THREAD_POOL;
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 4;
      bias = 2_000;
      messages = 20_000_000;
      seedMessages = 2_000;
      warmupFrac = .25f;
      log = new LogConfig() {{
        summary = stages = true;
      }};
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.test();
  }
}

