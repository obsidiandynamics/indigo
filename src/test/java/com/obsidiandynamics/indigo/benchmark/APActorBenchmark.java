package com.obsidiandynamics.indigo.benchmark;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.benchmark.APActor.*;

public final class APActorBenchmark {
  public static void main(String[] args) {
    final int threads = Runtime.getRuntime().availableProcessors() * 1;
    final ForkJoinPool executor = (ForkJoinPool) Executors.newWorkStealingPool(threads);
//    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    final int n = 200_000_000;
    
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
    final String m = "hi";
    for (int i = 0; i < messages; i++) {
      address.tell(m);
    }
  }
  
  private static class Counter {
    int value;
  }
  
  private static Address countingActor(int messages, ForkJoinPool executor, CountDownLatch latch) {
    return APActor.create(address -> {
      final Counter counter = new Counter();
      counter.value = messages;
      return m -> {
        counter.value--;
        //System.out.format("%x value=%d\n", System.identityHashCode(address), counter.value);
        if (counter.value == 0) {
          latch.countDown();
        }
        return APActor.stay;
      };
    }, executor, 1000);
  }
}
