package com.obsidiandynamics.indigo.experimental;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class ListTester {
  public static void main(String[] args) {
    final int steps = 1_000_000;
    final int stepSize = 1_000;
    final int threads = Runtime.getRuntime().availableProcessors();
    
    System.out.format("Running benchmark...\n");
    System.out.format("steps=%,d, stepSize=%,d\n", steps, stepSize);

    final CountDownLatch latch = new CountDownLatch(threads);
    final long took = TestSupport.took(() -> {
      for (int t = 0; t < threads; t++) {
        new Thread() {
          @Override public void run() {
            final Deque<Object> list = new ArrayDeque<>();
            for (int i = 0; i < steps; i++) {
              for (int j = 0; j < stepSize; j++) {
                final Object obj = new Object();
                list.addLast(obj);
              }
              
              for (int j = 0; j < stepSize; j++) {
                final Object first = list.removeFirst();
                first.hashCode();
              }
            }
            latch.countDown();
          }
        }.start();
      }
      
      try {
        latch.await();
      } catch (Exception e) {}
    });

    System.out.format("Took %,d s, %,d obj/s\n", 
                      took / 1000, (long) threads * steps * stepSize / took * 1000);
  }
}
