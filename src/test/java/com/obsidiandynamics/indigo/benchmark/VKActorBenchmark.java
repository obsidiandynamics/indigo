package com.obsidiandynamics.indigo.benchmark;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.benchmark.VKBatchActor.*;

public final class VKActorBenchmark {
  
  public static void main(String[] args) {
    final int threads = Runtime.getRuntime().availableProcessors();
    final ExecutorService executor = Executors.newWorkStealingPool(threads);
    final int n = 40_000_000;
    
    final long took = TestSupport.took(() -> {
      final CountDownLatch latch = new CountDownLatch(threads);
      for (int t = 0; t < threads; t++) {
        new Thread(() -> {
          final Address a = countingActor(n, executor, latch);
          send(a, n);
        }).start();
      }
      
      try {
        latch.await();
      } catch (Exception e) {}
    });
    executor.shutdown();
    
    System.out.format("%,d took %,d s, %,d ops/sec\n",
                      threads * n, took / 1000, threads * n / took * 1000);
  }
  
  private static void send(Address address, int messages) {
    final Object m = new Object();
    for (int i = 0; i < messages; i++) {
      address.tell(m);
    }
  }
  
  private static class Counter {
    int value;
  }
  
  private static Address countingActor(int messages, Executor executor, CountDownLatch latch) {
    return VKBatchActor.create(address -> {
      final Counter counter = new Counter();
      return m -> {
        counter.value++;
        if (counter.value == messages) {
          latch.countDown();
        }
        return VKBatchActor.stay;
      };
    }, executor);
  }
}
