package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.util.*;

public final class ThroughputBenchmark implements TestSupport {
  abstract static class Config implements Spec {
    ExecutorChoice executorChoice = null;
    long n;
    int threads;
    int actors;
    int bias;
    float warmupFrac;
    LogConfig log;
    
    /* Derived fields. */
    long warmup;
    
    @Override
    public void init() {
      warmup = (long) (n * warmupFrac);
    }

    @Override
    public LogConfig getLog() {
      return log;
    }

    @Override
    public String describe() {
      return String.format("%d threads, %,d receive actors, %,d messages/actor, %.0f%% warmup fraction", 
                           threads, actors, n, warmupFrac * 100);
    }

    @Override
    public Summary run() {
      return new ThroughputBenchmark().test(this);
    }
  }  
  
  @Test
  public void test() throws Exception {
    new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = 4;
      bias = 1_000;
      n = 1_000;
      warmupFrac = .05f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
    }}.test();
  }
  
  private Summary test(Config c) {
    final String SINK = "sink";
    
    final CountDownLatch latch = new CountDownLatch(c.actors);
    final long n = c.n;
    final ActorSystem system = new TestActorSystemConfig() {{
      if (c.executorChoice != null) {
        executor = c.executorChoice;
      }
      parallelism = c.threads;
      reaperPeriodMillis = 0;
      defaultActorConfig = new ActorConfig() {{
        bias = c.bias;
        backlogThrottleCapacity = Integer.MAX_VALUE;
        backlogThrottleTries = 10;
      }};
    }}
    .createActorSystem()
    .on(SINK).cue(IntegerState::new, (a, m, s) -> {
      if (++s.value == n) {
        latch.countDown();
      }
    });
    
    final ActorRef[] refs = new ActorRef[c.actors];
    
    for (int i = 0; i < c.actors; i++) {
      refs[i] = ActorRef.of(SINK, String.valueOf(i));
      system.tell(refs[i]);
    }
    
    if (c.warmup != 0) {
      if (c.log.stages) c.log.out.format("Warming up...\n");
      ParallelJob.blocking(c.actors, i -> {
        final ActorRef to = refs[i];
        final Message m = Message.builder().to(to).build();
        for (int j = 0; j < c.warmup; j++) {
          system.send(m);
        }
      }).run();
    
      try {
        system.drain(0);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    
    if (c.log.stages) c.log.out.format("Starting timed run...\n");
    final long o = n - c.warmup;
    final long took = TestSupport.took(
      ParallelJob.blocking(c.actors, i -> {
        final ActorRef to = refs[i];
        final Message m = Message.builder().to(to).build();
        for (int j = 0; j < o; j++) {
          system.send(m);
        }
        TestSupport.await(latch);
      })
    );
    
    system.shutdownSilently();
    
    final Summary summary = new Summary();
    summary.timedOps = o * c.actors;
    summary.avgTime = took;
    return summary;
  }
  
  public static void main(String[] args) throws Exception {
    new Config() {{
      executorChoice = ActorSystemConfig.ExecutorChoice.FIXED_THREAD_POOL;
      threads = Runtime.getRuntime().availableProcessors() * 1;
      actors = threads * 1;
      n = 10_000_000;
      warmupFrac = .05f;
      bias = 10_000;
      log = new LogConfig() {{
        summary = true;
      }};
    }}.testPercentile(9, 21, 50, Summary::byThroughput);
  }
}
