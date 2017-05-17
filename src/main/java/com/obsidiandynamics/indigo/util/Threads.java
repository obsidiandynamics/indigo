package com.obsidiandynamics.indigo.util;

import java.util.concurrent.*;
import java.util.concurrent.ForkJoinPool.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public final class Threads {
  private Threads() {}
  
  public static void throttle(BooleanSupplier test, int tries, long penaltyMillis) {
    try {
      // If we throttle messages with insufficient threads in the pool, then poor throughput is possible
      // due to starvation; hence we throttle in a ManagedBlocker which may add threads as required.
      // Note: this only works for the fork-join pool. A fixed thread pool will not be expanded
      // automatically.
      ForkJoinPool.managedBlock(new ManagedBlocker() {
        private int triesLeft = tries;
        
        @Override public boolean block() throws InterruptedException {
          Thread.sleep(penaltyMillis);
          return --triesLeft <= 0;
        }

        @Override public boolean isReleasable() {
          return triesLeft <= 0 || ! test.getAsBoolean();
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  public static ThreadPoolExecutor prestartedFixedThreadPool(int numThreads) {
    final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
    pool.prestartAllCoreThreads();
    return pool;
  }
  
  public static ForkJoinPool cappedForkJoinPool(int parallelism, JvmVersion version) {
    if (CappedForkJoinPool.isSafeFor(version)) {
      return new CappedForkJoinPool(parallelism, null, true);
    } else {
      throw new UnsupportedOperationException("ForkJoinPool cannot be used with this JVM version");
    }
  }
  
  public static ExecutorService autoPool(int parallelism, JvmVersion version) {
    return CappedForkJoinPool.isSafeFor(version) ? new CappedForkJoinPool(parallelism, null, true) : prestartedFixedThreadPool(parallelism);
  }
  
  public static Thread asyncDaemon(Runnable r, String threadName) {
    final Thread t = new Thread(r, threadName);
    t.setDaemon(true);
    t.start();
    return t;
  }
}
