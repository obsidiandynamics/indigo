package com.obsidiandynamics.indigo.benchmark;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class ThroughputBenchmark {
  private static final String ECHO = "echo";
  
  public static void main(String[] args) {
    final int threads = Runtime.getRuntime().availableProcessors() * 1;
    final int actors = threads * 1;
    final int n = 50_000_000;
    
    final CountDownLatch latch = new CountDownLatch(actors);
    final ActorSystem system = new ActorSystemConfig() {{
      parallelism = threads;
      executor = FORK_JOIN_POOL;
      defaultActorConfig = new ActorConfig() {{
        bias = 10_000;
        backlogThrottleCapacity = Integer.MAX_VALUE;
        backlogThrottleTries = 1;
      }};
    }}
    .define()
    .when(ECHO).lambda(IntegerState::new, (a, s) -> {
      if (++s.value == n) {
        latch.countDown();
      }
    });
    
    final long took = TestSupport.took(() -> {
      for (int i = 0; i < actors; i++) {
        final int _i = i;
        new Thread(() -> {
          final ActorRef to = ActorRef.of(ECHO, String.valueOf(_i));
          for (int j = 0; j < n; j++) {
            system.tell(to);
          }
        }).start();
      }
      
      try {
        latch.await();
      } catch (Exception e) {}
    });
    
    system.shutdown();
    System.out.format("%,d took %,d s, %,d ops/sec\n",
                      actors * n, took / 1000, actors * n / took * 1000);
  }
}
