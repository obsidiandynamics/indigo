package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static java.lang.Math.*;

import java.lang.Thread.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public final class CappedForkJoinPool extends ForkJoinPool {
  /** How much to scale the given parallelism factor by. E.g. for a given parallelism of 8
   *  and scale of 10, a maximum of 80 threads will be created. */
  private static final int SCALE = get("indigo.fjp.scale", Integer::parseInt, 10);
  
  /** An absolute cap on the number of threads in the pool. */
  private static final int MAX_THREADS = get("indigo.fjp.maxThreads", Integer::parseInt, 1024);
  
  /**
   *  Tests if the FJP is safe for use with the given JVM version. Specifically, it tests
   *  for the presence of the bug JDK-8078490, which can stall submissions to the FJP.
   *  
   *  @param version The version.
   *  @return True if the FJP is safe for use with this version, false otherwise.
   */
  public static boolean isSafeFor(JvmVersion version) {
    return version.compareTo(new JvmVersion(1, 8, 0, 40)) < 0 ||
        version.compareTo(new JvmVersion(1, 8, 0, 65)) >= 0;
  }
  
  public CappedForkJoinPool(int parallelism,
                            UncaughtExceptionHandler handler,
                            boolean asyncMode) {
    super(min(parallelism, MAX_THREADS), 
          pool -> pool.getPoolSize() < min(parallelism * SCALE, MAX_THREADS) 
              ? defaultForkJoinWorkerThreadFactory.newThread(pool) 
              : null, 
          handler, 
          asyncMode);
  }
}