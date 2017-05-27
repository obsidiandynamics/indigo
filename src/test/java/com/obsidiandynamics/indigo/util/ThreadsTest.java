package com.obsidiandynamics.indigo.util;

import org.junit.*;

public final class ThreadsTest implements TestSupport {
  @Test
  public void testThrottleTries() {
    Threads.throttle(() -> true, 0, 1);
    Threads.throttle(() -> true, 1, 1);
  }
  
  @Test
  public void testThrottleCondition() {
    Threads.throttle(() -> false, Integer.MAX_VALUE, 1);
  }
}
