package com.obsidiandynamics.indigo;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.*;

public final class ThrottleTest implements TestSupport {
  private static final String SINK = "sink";
  
  /**
   *  Tests soft-throttling backpressure, whereby the enqueuing thread is subjected to a finite
   *  time penalty when the target actor has a full backlog (but the enqueuing thread isn't blocked
   *  indefinitely).
   */
  @Test
  public void test() {
    final int runs = 20;
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch end = new CountDownLatch(runs);

    final ActorSystem system = new TestActorSystemConfig() {{
      defaultActorConfig = new ActorConfig() {{
        backlogThrottleCapacity = 10;
        backlogThrottleMillis = 1;
        backlogThrottleTries = 1;
      }};
    }}
    .define()
    .when(SINK).lambda((a, m) -> {
      log("waiting to start\n");
      TestSupport.await(start);
      log("started\n");
      end.countDown();
    })
    .ingress().act(a -> { 
      for (int i = 0; i < runs; i++) {
        a.to(ActorRef.of(SINK)).tell(i);
      }
      
      log("finished\n");
      start.countDown();
    });
    
    try {
      if (! end.await(10, TimeUnit.SECONDS)) {
        fail("Latch didn't count down");
      }
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    
    system.shutdownQuietly();
  }
}
