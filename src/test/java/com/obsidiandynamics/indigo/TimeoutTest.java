package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class TimeoutTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String DONE = "done";
  
  private static final int MIN_TIMEOUT = 10;
  private static final int MAX_TIMEOUT = 200;
  private static final int TIMEOUT_TOLERANCE = 50;
  
  private static final long generateRandomTimeout() {
    final int range = MAX_TIMEOUT - MIN_TIMEOUT;
    return MIN_TIMEOUT + (int) (Math.random() * range);
  }

  @Test
  public void test() {
    logTestName();
    
    final int actors = 1000;
    final Set<ActorRef> done = new HashSet<>();

    new ActorSystemConfig() {}
    .define()
    .when(DRIVER).lambda(a -> {
      final long startTime = System.currentTimeMillis();
      final long timeout = generateRandomTimeout();
      a.to(ActorRef.of(ECHO)).ask().allow(timeout)
      .onTimeout(t -> {
        final long elapsed = System.currentTimeMillis() - startTime;
        final long timeDiff = Math.abs(elapsed - timeout);
        System.out.println("timed out " + a.self() + " diff=" + timeDiff + " t/o=" + timeout);
        
        assertTrue(String.format("Timeout not within threshold: elapsed %d, expected: %d", elapsed, timeout), 
                   timeDiff <= TIMEOUT_TOLERANCE);
        t.to(ActorRef.of(DONE)).tell();
      })
      .onResponse(r -> {
        fail(String.format("Got unexpected response, timeout set to %d", timeout));
      });
    })
    .when(ECHO).lambda(a -> { /* do nothing, stalling the reply */ })
    .when(DONE).lambda(refCollector(done))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        a.to(ActorRef.of(DRIVER, i + "")).tell();
      }
    })
    .shutdown();

    assertEquals(actors, done.size());
  }
}

