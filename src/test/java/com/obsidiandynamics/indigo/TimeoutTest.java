package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class TimeoutTest implements TestSupport {
  public static final String KEY_TIMEOUT_TOLERANCE = "indigo.TimeoutTest.timeoutTolerance";
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String DONE = "done";
  
  private static final int MIN_TIMEOUT = 10;
  private static final int MAX_TIMEOUT = 100;
  
  private static final long generateRandomTimeout() {
    final int range = MAX_TIMEOUT - MIN_TIMEOUT;
    return MIN_TIMEOUT + (int) (Math.random() * range);
  }

  @Test
  public void test() {
    logTestName();
    
    final int actors = 100;
    final Map<ActorRef, Long> done = new HashMap<>();

    new TestActorSystemConfig() {}
    .define()
    .when(DRIVER).lambda((a, m) -> {
      final long startTime = System.currentTimeMillis();
      final long timeout = generateRandomTimeout();
      a.to(ActorRef.of(ECHO)).ask().await(timeout)
      .onTimeout(() -> {
        final long elapsed = System.currentTimeMillis() - startTime;
        final long timeDiff = Math.abs(elapsed - timeout);
        log("Timed out %s, diff=%d, t/o=%d, actual=%d\n", a.self(), timeDiff, timeout, elapsed);
        
        a.to(ActorRef.of(DONE)).tell(timeDiff);
      })
      .onResponse(r -> {
        fail(String.format("Got unexpected response, timeout set to %d", timeout));
      });
    })
    .when(ECHO).lambda((a, m) -> { /* do nothing, stalling the reply */ })
    .when(DONE).lambda((a, m) -> done.put(m.from(), m.body()))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdownQuietly();

    assertEquals(actors, done.size());
    long totalDiff = 0;
    for (long diff : done.values()) {
      totalDiff += diff; 
    }
    final double avgDiff = (double) totalDiff / actors;
    
    final int timeoutTolerance = get(KEY_TIMEOUT_TOLERANCE, Integer::parseInt, 10);
    log("Average diff: %.1f\n", avgDiff);
    assertTrue(String.format("Average timeout threshold above tolerance: %.1f", avgDiff), 
               avgDiff <= timeoutTolerance);
  }
}