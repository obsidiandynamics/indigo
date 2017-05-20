package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

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
    final CountDownLatch latch = blocking ? new CountDownLatch(threads) : null;
    final CyclicBarrier barrier = new CyclicBarrier(threads + 1);
    final String threadNameFormat = "ParRunner-%0" + numDigits(threads) + "d";
    for (int i = 0; i < threads; i++) {
      final int _i = i;
      final Thread t = new Thread(() ->  {
        TestSupport.await(barrier);
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
  
  public static <T> ParallelJob blockingSlice(List<T> list, int threads, Consumer<List<T>> task) {
    return slice(list, threads, true, task);
  }
  
  private static <T> ParallelJob slice(List<T> list, int threads, boolean blocking, Consumer<List<T>> task) {
    final int actualThreads = Math.min(threads, list.size());
    final List<List<T>> lists = new ArrayList<>(actualThreads);
    int pos = 0;
    for (int i = 0; i < actualThreads; i++) {
      final int remaining = actualThreads - i;
      final int len = (list.size() - pos) / remaining;
      lists.add(list.subList(pos, pos + len));
      pos += len;
    }
    assert pos == list.size() : "pos=" + pos + ", list.size=" + list.size();
    
    return create(actualThreads, blocking, i -> {
      final List<T> sublist = lists.get(i);
      task.accept(sublist);
    });
  }
  
  @Override
  public void run() {
    TestSupport.await(barrier);
    if (latch != null) TestSupport.await(latch);
  }
}