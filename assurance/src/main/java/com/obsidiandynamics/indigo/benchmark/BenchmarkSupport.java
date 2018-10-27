package com.obsidiandynamics.indigo.benchmark;

public interface BenchmarkSupport {
  static void forceGC() {
    System.gc();
  }
}
