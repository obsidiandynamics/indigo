package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class ThroughputBenchmark {
  private static void benchmark() {
    final String SINK = "sink";
    
    final int threads = Runtime.getRuntime().availableProcessors() * 1;
    final int actors = threads * 8;
    final long n = 20_000_000;
    
    final CountDownLatch latch = new CountDownLatch(actors);
    final ActorSystem system = new ActorSystemConfig() {{
      parallelism = threads;
      executor = FORK_JOIN_POOL;
      defaultActorConfig = new ActorConfig() {{
        bias = 10_000;
        //backlogThrottleCapacity = Integer.MAX_VALUE;
        backlogThrottleTries = 1;
      }};
    }}
    .define()
    .when(SINK).lambda(IntegerState::new, (a, s) -> {
      if (++s.value == n) {
        latch.countDown();
      }
    });
    
    final long took = TestSupport.took(() ->
      TestSupport.parallel(actors, latch, i -> {
        final ActorRef to = ActorRef.of(SINK, String.valueOf(i));
        for (int j = 0; j < n; j++) {
          system.tell(to);
        }
      })
    );
    
    system.shutdown();
    System.out.format("%,d took %,d s, %,d ops/sec\n",
                      actors * n, took / 1000, actors * n / took * 1000);
  }
  
  public static void main(String[] args) {
    System.out.println("bench started");
    for (int i = 0; i< 5; i++) {
      System.gc();
      benchmark();
    }
  }
}
