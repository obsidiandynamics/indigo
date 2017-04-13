package com.obsidiandynamics.indigo.experimental;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.experimental.VKBatchActor.*;
import com.obsidiandynamics.indigo.util.*;

public final class VKActorBenchmark {
  private static void benchmark() {
    final int threads = Runtime.getRuntime().availableProcessors();
    final ExecutorService executor = Executors.newWorkStealingPool(threads);
    final int n = 10_000_000;

    final CountDownLatch latch = new CountDownLatch(threads);
    final long took = TestSupport.took(() -> {
      ParallelJob.blocking(threads, i -> {
        send(countingActor(n, executor, latch), n);
        Threads.await(latch);
      }).run();
    });
    executor.shutdown();
    
    System.out.format("%,d took %,d s, %,d ops/sec\n",
                      threads * n, took / 1000, threads * n / took * 1000);
  }
  
  public static void main(String[] args) {
    System.out.println("bench started");
    for (int i = 0; i < 29; i++) {
      BenchmarkSupport.forceGC();
      benchmark();
    }
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
