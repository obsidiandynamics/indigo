package com.obsidiandynamics.indigo.util;

import java.lang.Thread.*;
import java.util.concurrent.*;

public final class CappedForkJoinPool extends ForkJoinPool {
  public CappedForkJoinPool(int parallelism,
                            UncaughtExceptionHandler handler,
                            int threadCap,
                            boolean asyncMode) {
    super(parallelism, 
          pool -> pool.getPoolSize() < threadCap ? defaultForkJoinWorkerThreadFactory.newThread(pool) : null, 
          handler, 
          asyncMode);
  }
}