package com.obsidiandynamics.indigo;

import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public final class ParallelJob implements Runnable {
  private static final boolean BLOCKING = true;
  private static final boolean NON_BLOCKING = false;
  
  private final CountDownLatch latch;
  private final CyclicBarrier barrier;
  
  private ParallelJob(CountDownLatch latch, CyclicBarrier barrier) {
    this.latch = latch;
    this.barrier = barrier;
  }
  
  public static ParallelJob blocking(int threads, Consumer<Integer> r) {
    return create(threads, BLOCKING, r);
  }
  
  public static ParallelJob nonBlocking(int threads, Consumer<Integer> r) {
    return create(threads, NON_BLOCKING, r);
  }
  
  private static ParallelJob create(int threads, boolean blocking, Consumer<Integer> r) {
    final CountDownLatch latch = new CountDownLatch(threads);
    final CyclicBarrier barrier = new CyclicBarrier(threads + 1);
    final String threadNameFormat = "ParRunner-%0" + numDigits(threads) + "d";
    for (int i = 0; i < threads; i++) {
      final int _i = i;
      final Thread t = new Thread(() ->  {
        Threads.await(barrier);
        r.accept(_i);
        if (latch != null) latch.countDown();
      }, String.format(threadNameFormat, i));
      t.start();
    }
    return new ParallelJob(latch, barrier);
  }
  
  private static int numDigits(int num) {
    return String.valueOf(num).length();
  }
  
  @Override
  public void run() {
    Threads.await(barrier);
    if (latch != null) Threads.await(latch);
  }
}