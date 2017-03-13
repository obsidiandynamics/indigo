package com.obsidiandynamics.indigo.experimental;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class ListTester {
  public static void main(String[] args) {
    final int steps = 1_000_000;
    final int stepSize = 100;
    final int threads = Runtime.getRuntime().availableProcessors();
    final boolean sync = false;
    
    System.out.format("Running benchmark...\n");
    System.out.format("steps=%,d, stepSize=%,d\n", steps, stepSize);

    final CountDownLatch latch = new CountDownLatch(threads);
    final long took = TestSupport.took(() -> {
      for (int t = 0; t < threads; t++) {
        new Thread(() -> {
          final Queue<Object> list = new ArrayDeque<>();
          for (int i = 0; i < steps; i++) {
            for (int j = 0; j < stepSize; j++) {
              final Object obj = new Object();
              if (sync) synchronized (list) {
                list.add(obj);
              } else {
                list.add(obj);
              }
            }
            
            for (int j = 0; j < stepSize; j++) {
              final Object first;
              if (sync) synchronized (list) {
                first = list.remove();
              } else {
                first = list.remove();
              }
              first.equals(first);
            }
          }
          latch.countDown();
        }).start();
      }
      
      try {
        latch.await();
      } catch (Exception e) {}
    });

    System.out.format("Took %,d s, %,d obj/s\n", 
                      took / 1000, (long) threads * steps * stepSize / took * 1000);
  }
}
