package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import org.junit.*;

public interface TestSupport {
  boolean LOG = get(load("system-test.properties", System.getProperties()),
                    "TestSupport.log", Boolean::parseBoolean, false);
  PrintStream LOG_STREAM = System.out;

  default void log(String format, Object ... args) {
    if (LOG) LOG_STREAM.printf(format, args);
  }

  static long took(Runnable r) {
    final long started = System.nanoTime();
    r.run();
    final long took = System.nanoTime() - started;
    return took / 1_000_000l;
  }

  static long tookThrowing(ThrowingRunnable r) throws Exception {
    final long started = System.nanoTime();
    r.run();
    final long took = System.nanoTime() - started;
    return took / 1_000_000l;
  }

  static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static void await(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (BrokenBarrierException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   *  Verifies that the given object overrides the <code>toString()</code> implementation, and that
   *  the implementation operates without throwing any exceptions.
   *
   *  @param obj The object to test.
   */
  static void assertToString(Object obj) {
    final String objectToString = obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
    final String actualToString = obj.toString();
    assertNotEquals(objectToString, actualToString);
  }

  /**
   *  Verifies that a utility class is well defined.
   *
   *  Taken from
   *  https://github.com/trajano/maven-jee6/blob/master/maven-jee6-test/src/test/java/net/trajano/maven_jee6/test/test/UtilityClassTestUtilTest.java
   *
   *  @param clazz Utility class to verify.
   */
  static void assertUtilityClassWellDefined(final Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Assert.assertTrue("Class must be final",
                      Modifier.isFinal(clazz.getModifiers()));
    Assert.assertEquals("There must be only one constructor", 1,
                        clazz.getDeclaredConstructors().length);
    final Constructor<?> constructor = clazz.getDeclaredConstructor();
    if (constructor.isAccessible() ||
        !Modifier.isPrivate(constructor.getModifiers())) {
      Assert.fail("Constructor is not private");
    }
    constructor.setAccessible(true);
    constructor.newInstance();
    constructor.setAccessible(false);
    for (final Method method : clazz.getMethods()) {
      if (! Modifier.isStatic(method.getModifiers())
          && method.getDeclaringClass().equals(clazz)) {
        Assert.fail("There exists a non-static method: " + method);
      }
    }
  }
}
