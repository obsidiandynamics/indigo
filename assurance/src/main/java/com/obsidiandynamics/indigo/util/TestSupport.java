package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.io.*;
import java.util.concurrent.*;

import com.obsidiandynamics.func.*;

public interface TestSupport {
  boolean LOG = get(load("system-test.properties", System.getProperties()),
                    "TestSupport.log", Boolean::parseBoolean, false);
  PrintStream LOG_STREAM = System.out;

  default void log(String format, Object ... args) {
    logStatic(format, args);
  }
  
  static void logStatic(String format, Object ... args) {
    if (LOG) LOG_STREAM.printf(format, args);
  }

  static long took(Runnable r) {
    final long started = System.nanoTime();
    r.run();
    final long took = System.nanoTime() - started;
    return took / 1_000_000l;
  }

  static long tookThrowing(CheckedRunnable<Throwable> r) throws Throwable {
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
}
