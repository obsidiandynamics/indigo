package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static java.lang.Math.*;

import java.lang.Thread.*;
import java.util.concurrent.*;

public final class CappedForkJoinPool extends ForkJoinPool {
  /** How much to scale the given parallelism factor by. E.g. for a given parallelism of 8
   *  and scale of 10, a maximum of 80 threads will be created. */
  private static final int SCALE = get("indigo.fjp.scale", Integer::parseInt, 10);
  
  /** An absolute cap on the number of threads in the pool. */
  private static final int MAX_THREADS = get("indigo.fjp.maxThrads", Integer::parseInt, 1024);
  
  public CappedForkJoinPool(int parallelism,
                            UncaughtExceptionHandler handler,
                            boolean asyncMode) {
    super(min(parallelism, MAX_THREADS), 
          pool -> {
            return pool.getPoolSize() < min(parallelism * SCALE, MAX_THREADS) 
                ? defaultForkJoinWorkerThreadFactory.newThread(pool) 
                : null;
          }, 
          handler, 
          asyncMode);
  }
}