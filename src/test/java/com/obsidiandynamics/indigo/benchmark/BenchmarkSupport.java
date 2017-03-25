package com.obsidiandynamics.indigo.benchmark;

import static java.lang.String.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.math3.stat.descriptive.*;

interface BenchmarkSupport {
  interface TimedState {
    long getTotalProcessed();
    
    long getTimeTaken();
  }
  
  static final class Timings {
    long timedTxns = 0;
    long avgTime = 0;
    final Stats stats = new Stats();
    
    void compute(Collection<? extends TimedState> states, int actors) {
      for (TimedState s : states) {
        timedTxns += s.getTotalProcessed();
        avgTime += s.getTimeTaken();
      }
      avgTime /= actors;
      stats.await();
    }
    
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(format("%,d txns took %,d s, %,d txn/s\n", timedTxns, avgTime / 1000, timedTxns / Math.max(1, avgTime) * 1000));
      if (stats.samples.getN() != 0) {
        sb.append(format("Latency: mean: %,.1f, sd: %,.1f, min: %,.1f, 50%%: %,.1f, 95%%: %,.1f, 99%%: %,.1f, max: %,.1f (µs, N=%,d)\n", 
                         stats.samples.getMean() / 1000, 
                         stats.samples.getStandardDeviation() / 1000,
                         stats.samples.getMin() / 1000,
                         stats.samples.getPercentile(5) / 1000, 
                         stats.samples.getPercentile(95) / 1000,
                         stats.samples.getPercentile(99) / 1000,
                         stats.samples.getMax() / 1000,
                         stats.samples.getN()));
      }
      return sb.toString();
    }
  }
  
  static class Stats {
    final DescriptiveStatistics samples = new DescriptiveStatistics();
    final ExecutorService executor = Executors.newFixedThreadPool(1);
    
    void await() {
      executor.shutdown();
      try {
        executor.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {}
    }
  }
  
  static void forceGC() {
    System.gc();
    /*for (int i = 0; i < 5; i++) {
      System.gc();
      Threads.sleep(200);
    }*/
  }
}
