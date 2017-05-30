package com.obsidiandynamics.indigo.util;

import java.util.function.*;

public final class Await {
  private Await() {}
  
  public static boolean await(int waitMillis, int intervalMillis, BooleanSupplier test) throws InterruptedException {
    final long maxWait = System.nanoTime() + waitMillis * 1_000_000l;
    boolean result;
    do {
      result = test.getAsBoolean();
      if (result) {
        return true;
      } else {
        Thread.sleep(intervalMillis);
      }
    } while (System.nanoTime() < maxWait);
    return false;
  }
}
