package com.obsidiandynamics.indigo.util;

import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public interface TestSupport {
  static final boolean LOG = false;
  static final PrintStream LOG_STREAM = System.out;
  
  default void log(String format, Object ... args) {
    if (LOG) LOG_STREAM.printf(format, args);
  }
  
  default BiConsumer<Activation, Message> refCollector(Set<ActorRef> set) {
    return (a, m) -> {
      log("Finished %s\n", m.from());
      set.add(m.from());
    };
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
  
  static int countFaults(FaultType type, Collection<Fault> deadLetterQueue) {
    int count = 0;
    for (Fault fault : deadLetterQueue) {
      if (fault.getType() == type) {
        count++;
      }
    }
    return count;
  }
  
  static Executor oneTimeExecutor(String threadName) {
    return r -> Threads.asyncDaemon(r, threadName);
  }
  
  static <I, O> EgressBuilder<I, O> egressMode(EgressBuilder<I, O> builder, boolean parallel) {
    if (parallel) builder.parallel(); else builder.serial();
    return builder;
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
