package com.obsidiandynamics.indigo.util;

import java.util.concurrent.*;
import java.util.concurrent.ForkJoinPool.*;
import java.util.function.*;

public final class Threads {
  private Threads() {};
  
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
    } catch (InterruptedException e) {}
  }
  
  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }  
  
  public static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void await(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static ThreadPoolExecutor prestartedFixedThreadPool(int numThreads) {
    final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
    pool.prestartAllCoreThreads();
    return pool;
  }
  
  public static ForkJoinPool cappedForkJoinPool(int parallelism) {
    return new CappedForkJoinPool(parallelism, null, true);
  }
}
