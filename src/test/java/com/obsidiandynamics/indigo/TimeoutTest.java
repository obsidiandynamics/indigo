package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class TimeoutTest implements TestSupport {
  public static final String KEY_TIMEOUT_TOLERANCE = "indigo.TimeoutTest.timeoutTolerance";
  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  private static final String DONE = "done";
  
  private static final int MIN_TIMEOUT = 10;
  private static final int MAX_TIMEOUT = 100;
  
  private ActorSystem system;
  
  @After
  public void teardown() {
    if (system != null) {
      system.shutdownSilently();
    }
  }
  
  private static final long generateRandomTimeout() {
    final int range = MAX_TIMEOUT - MIN_TIMEOUT;
    return MIN_TIMEOUT + (int) (Math.random() * range);
  }

  @Test
  public void test() {
    final int actors = 100;
    final Map<ActorRef, Long> done = new HashMap<>();

    system = new TestActorSystemConfig() {}
    .createActorSystem()
    .on(DRIVER).cue((a, m) -> {
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
    .on(ECHO).cue((a, m) -> { /* do nothing, stalling the reply */ })
    .on(DONE).cue((a, m) -> done.put(m.from(), m.body()))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell());
    
    system.shutdownSilently();

    assertEquals(actors, done.size());
    long totalDiff = 0;
    for (long diff : done.values()) {
      totalDiff += diff; 
    }
    final double avgDiff = (double) totalDiff / actors;
    
    final int timeoutTolerance = get(KEY_TIMEOUT_TOLERANCE, Integer::parseInt, 50);
    log("Average diff: %.1f\n", avgDiff);
    assertTrue(String.format("Average timeout threshold above tolerance: %.1f", avgDiff), 
               avgDiff <= timeoutTolerance);
  }
  
  @Test(expected=IllegalStateException.class)
  public void testNoTimeoutScheduler() {
    system = new TestActorSystemConfig() {{
      enableTimeoutScheduler = false;
    }}.createActorSystem();

    system.getTimeoutScheduler();
  }
}