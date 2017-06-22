package com.obsidiandynamics.indigo.benchmark;

public interface BenchmarkSupport {
  static void forceGC() {
    System.gc();
    /*for (int i = 0; i < 5; i++) {
      System.gc();
      Threads.sleep(200);
    }*/
  }
}
