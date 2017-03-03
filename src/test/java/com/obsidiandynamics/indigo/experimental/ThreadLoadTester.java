package com.obsidiandynamics.indigo.experimental;

import java.util.*;
import java.util.concurrent.*;

public final class ThreadLoadTester {
  public static void main(String[] args) {
    final int threads = 8;
    final int runs = 10_000_000;
    final int throttle = 10_000;
    final int throttleBackoff = 10;
    
    @SuppressWarnings("unchecked")
    final LinkedList<Object>[] lists = (LinkedList<Object>[]) new LinkedList<?>[threads];
    for (int i = 0; i < lists.length; i++) {
      lists[i] = new LinkedList<>();
    }
    
    final CountDownLatch latch = new CountDownLatch(threads);
    
    for (int i = 0; i < threads; i++) {
      final int threadId = i;
      new Thread("Thread-" + threadId) {
        final LinkedList<?> list = lists[threadId];
        @Override public void run() {
          for (int j = 0; j < runs; j++) {
            synchronized (list) {
              if (! list.isEmpty()) {
                list.removeFirst();
              } else {
                try {
                  list.wait(10);
                } catch (InterruptedException e) {}
              }
            }
          }
          latch.countDown();
        }
      }.start();
    }
    
    final long start = System.currentTimeMillis();
    
    final Object obj = new Object();
    for (int j = 0; j < runs; j++) {
      for (int i = 0; i < threads; i++) {
        final LinkedList<Object> list = lists[i];
        synchronized (list) {
          if (list.size() < throttle) {
            list.addLast(obj);
          } else {
            try {
              list.wait(throttleBackoff);
            } catch (InterruptedException e) {}
          }
        }
      }
    }
    
    try {
      latch.await();
    } catch (InterruptedException e) {}
    
    final long took = System.currentTimeMillis() - start;
    System.out.format("Took %d s\n", took / 1000);
  }
}
