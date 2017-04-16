package com.obsidiandynamics.indigo.benchmark;

import static java.lang.String.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.math3.stat.descriptive.*;

final class Summary {
  long timedOps = 0;
  long avgTime = 0;
  final Stats stats = new Stats();

  void compute(Collection<? extends Elapsed> states, int actors) {
    for (Elapsed s : states) {
      timedOps += s.getTotalProcessed();
      avgTime += s.getTimeTaken();
    }
    avgTime /= actors;
    stats.await();
  }

  static final class Stats {
    final DescriptiveStatistics samples = new DescriptiveStatistics();
    final ExecutorService executor = Executors.newFixedThreadPool(1);

    void await() {
      executor.shutdown();
      try {
        executor.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(format("%,d ops took %,d ms, %,d op/s", timedOps, avgTime, timedOps / Math.max(1, avgTime) * 1000));
    if (stats.samples.getN() != 0) {
      sb.append(format("\nLatency: mean: %,.1f, sd: %,.1f, min: %,.1f, 50%%: %,.1f, 95%%: %,.1f, 99%%: %,.1f, max: %,.1f (µs, N=%,d)", 
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
  
  public static int byThroughput(Summary s1, Summary s2) {
    return Long.compare(s2.avgTime, s1.avgTime);
  }
  
  public static int byLatency(Summary s1, Summary s2) {
    return Double.compare(s2.stats.samples.getPercentile(50), s1.stats.samples.getPercentile(50));
  }
}