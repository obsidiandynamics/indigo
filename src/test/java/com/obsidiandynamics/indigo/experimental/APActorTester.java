package com.obsidiandynamics.indigo.experimental;

import static com.obsidiandynamics.indigo.experimental.APActor.*;

import java.util.concurrent.*;

public final class APActorTester {
  private static class Counter {
    int value;
  }
  
  private static Address createActor(CountDownLatch latch, int expected, ForkJoinPool e) {
    return APActor.create(addr -> {
      final Counter counter = new Counter();
      return m -> {
        counter.value++;
        //System.out.format("%x got %08d (%d)\n", System.identityHashCode(addr), m, counter.value);
        if (counter.value == expected) {
          System.out.format("%x is done\n", System.identityHashCode(addr));
          latch.countDown();
        }
        return stay;
      };
    }, e, 100);
  }
  
  public static void main(String[] args) throws InterruptedException {
    final int actorThreads = Runtime.getRuntime().availableProcessors();
    final int driverThreads = 8;
    final int n = 1_000_000;
    
    final ForkJoinPool e = (ForkJoinPool) Executors.newWorkStealingPool(actorThreads);
    final CountDownLatch latch = new CountDownLatch(driverThreads);
    for (int d = 0; d < driverThreads; d++) {
      final int _d = d;
      new Thread(() -> {
        final Address a = createActor(latch, n, e);
        for (int i = 0; i < n; i++) {
          a.tell(_d * 1_000_000 + i);
        }
      }).start();
    }
    
    latch.await();
    e.shutdown();
  }
}
