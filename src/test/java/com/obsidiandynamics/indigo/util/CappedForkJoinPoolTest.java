package com.obsidiandynamics.indigo.util;

import static java.util.concurrent.TimeUnit.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public final class CappedForkJoinPoolTest implements TestSupport {
  @Test
  public void testSafeVersions() {
    assertTrue(CappedForkJoinPool.isSafeFor(new JvmVersion(1, 8, 0, 39)));
    assertTrue(CappedForkJoinPool.isSafeFor(new JvmVersion(1, 8, 0, 65)));
  }

  @Test
  public void testUnsafeVersions() {
    assertFalse(CappedForkJoinPool.isSafeFor(new JvmVersion(1, 8, 0, 40)));
    assertFalse(CappedForkJoinPool.isSafeFor(new JvmVersion(1, 8, 0, 64)));
  }

  @Test
  public void testCappedThreadFactory() {
    testCappedThreadFactory(1);
    testCappedThreadFactory(2);
    testCappedThreadFactory(4);
    testCappedThreadFactory(8);
  }

  private void testCappedThreadFactory(int cap) {
    final int initialParallelism = 1;
    final int submissions = cap * 2;

    final ForkJoinPool pool = new CappedForkJoinPool(initialParallelism, 10, cap, null, true);
    try {
      final AtomicBoolean complete = new AtomicBoolean();

      for (int i = 0; i < submissions; i++) {
        pool.execute(() -> Threads.throttle(() -> ! complete.get(), Integer.MAX_VALUE, 1));
      }

      await().atMost(10, SECONDS).until(() -> pool.getQueuedSubmissionCount() >= 1 || pool.getPoolSize() >= 1);

      // note: the reported pool size may be one higher than the true number of active threads, as the former
      // is incremented just prior to attempting to add a new thread
      assertTrue("poolSize=" + pool.getPoolSize(), pool.getPoolSize() <= cap + 1);
      complete.set(true);
    } finally {
      pool.shutdownNow();
    }
  }
}
