package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;

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
}
