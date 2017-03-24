package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorConfig.ActivationChoice.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class ThroughputBenchmark {
  private static void benchmark() {
    final String SINK = "sink";
    
    final int threads = Runtime.getRuntime().availableProcessors() * 1;
    final int actors = threads * 1;
    final long n = 10_000_000;
    final int warmup = (int) (n * .05);
    
    final CountDownLatch latch = new CountDownLatch(actors);
    final ActorSystem system = new ActorSystemConfig() {{
      parallelism = threads;
      executor = FORK_JOIN_POOL;
      defaultActorConfig = new ActorConfig() {{
        bias = 10_000;
        backlogThrottleCapacity = Integer.MAX_VALUE;
        backlogThrottleTries = 10;
        activationFactory = NODE_QUEUE;
      }};
    }}
    .define()
    .when(SINK).lambda(IntegerState::new, (a, s) -> {
      if (++s.value == n) {
        latch.countDown();
      }
    });
    
    final ActorRef[] refs = new ActorRef[actors];
    
    for (int i = 0; i < actors; i++) {
      refs[i] = ActorRef.of(SINK, String.valueOf(i));
      system.tell(refs[i]);
    }
    
    ParallelJob.create(actors, null, i -> {
      final ActorRef to = refs[i];
      final Message m = Message.builder().to(to).build();
      for (int j = 0; j < warmup; j++) {
        system.send(m);
      }
    }).run();
    
    try {
      system.drain();
    } catch (InterruptedException e) {}
    
    /*for (int i = 0; i < 5; i++) {
      System.gc();
      Threads.sleep(200);
    }*/
    
    final long o = n - warmup;
    final long took = TestSupport.took(
      ParallelJob.create(actors, latch, i -> {
        final ActorRef to = refs[i];
        final Message m = Message.builder().to(to).build();
        for (int j = 0; j < o; j++) {
          system.send(m);
        }
      })
    );
    
    system.shutdown();
    System.out.format("%,d took %,d s, %,d ops/sec\n",
                      actors * o, took / 1000, actors * o / took * 1000);
  }
  
  public static void main(String[] args) {
    System.out.println("bench started");
    for (int i = 0; i < 29; i++) {
      System.gc();
      Threads.sleep(1000);
      benchmark();
    }
  }
}
