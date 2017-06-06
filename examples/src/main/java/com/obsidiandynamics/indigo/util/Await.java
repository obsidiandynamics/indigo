package com.obsidiandynamics.indigo.util;

import java.util.concurrent.*;
import java.util.function.*;

public final class Await {
  private static final int DEF_INTERVAL = 10;
  
  private Await() {}
  
  public static boolean perpetual(BooleanSupplier test) throws InterruptedException {
    return bounded(Integer.MAX_VALUE, DEF_INTERVAL, test);
  }
  
  public static boolean perpetual(int intervalMillis, BooleanSupplier test) throws InterruptedException {
    return bounded(Integer.MAX_VALUE, intervalMillis, test);
  }
  
  public static void boundedTimeout(int waitMillis, BooleanSupplier test) throws InterruptedException, TimeoutException {
    boundedTimeout(waitMillis, DEF_INTERVAL, test);
  }
  
  public static boolean bounded(int waitMillis, BooleanSupplier test) throws InterruptedException {
    return bounded(waitMillis, DEF_INTERVAL, test);
  }
  
  public static void boundedTimeout(int waitMillis, 
                                    int intervalMillis, 
                                    BooleanSupplier test) throws InterruptedException, TimeoutException {
    if (! bounded(waitMillis, intervalMillis, test)) {
      throw new TimeoutException(String.format("Timed out after %,d ms", waitMillis));
    }
  }
  
  public static boolean bounded(int waitMillis, int intervalMillis, BooleanSupplier test) throws InterruptedException {
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
