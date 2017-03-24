package com.obsidiandynamics.indigo;

import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public final class ParallelJob implements Runnable {
  private final CountDownLatch latch;
  private final CyclicBarrier barrier;
  
  private ParallelJob(CountDownLatch latch, CyclicBarrier barrier) {
    this.latch = latch;
    this.barrier = barrier;
  }
  
  public static ParallelJob create(int threads, Consumer<Integer> r) {
    return create(threads, null, r);
  }

  public static ParallelJob create(int threads, CountDownLatch latch, Consumer<Integer> r) {
    final CyclicBarrier barrier = new CyclicBarrier(threads + 1);
    for (int i = 0; i < threads; i++) {
      final int _i = i;
      final Thread t = new Thread(() ->  {
        Threads.await(barrier);
        r.accept(_i); 
      });
      t.start();
    }
    return new ParallelJob(latch, barrier);
  }
  
  @Override
  public void run() {
    Threads.await(barrier);
    if (latch != null) Threads.await(latch);
  }
}