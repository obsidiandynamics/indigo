package com.obsidiandynamics.indigo.experimental;

public final class SleepTester {
  public static void main(String[] args) {
    final Object lock = new Object();
    final long start = System.currentTimeMillis();
    final long sleep = 30;
    synchronized (lock) {
      try {
        lock.wait(sleep, 0);
      } catch (InterruptedException e) {}
    }
    
    System.out.format("Took %d\n", System.currentTimeMillis() - start);
  }
}
