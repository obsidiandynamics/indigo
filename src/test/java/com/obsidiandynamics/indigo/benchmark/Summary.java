package com.obsidiandynamics.indigo.benchmark;

import static java.lang.String.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.math3.stat.descriptive.*;

public final class Summary {
  public long timedOps = 0;
  public long avgTime = 0;
  public final Stats stats = new Stats();

  public void compute(Collection<? extends Elapsed> intervals) {
    for (Elapsed interval : intervals) {
      timedOps += interval.getTotalProcessed();
      avgTime += interval.getTimeTaken();
    }
    avgTime /= intervals.size();
    stats.await();
  }

  public static final class Stats {
    public final DescriptiveStatistics samples = new DescriptiveStatistics();
    public final ExecutorService executor = Executors.newFixedThreadPool(1);

    public void await() {
      executor.shutdown();
      try {
        while (! executor.awaitTermination(1, TimeUnit.MINUTES));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(format("%,d ops took %,d ms, %,.0f op/s", timedOps, avgTime, timedOps / Math.max(1f, avgTime) * 1000));
    if (stats.samples.getN() != 0) {
      sb.append(format("\nLatency: mean: %,.1f, sd: %,.1f, min: %,.1f, 50%%: %,.1f, 95%%: %,.1f, 99%%: %,.1f, max: %,.1f (µs, N=%,d)", 
                       stats.samples.getMean() / 1000, 
                       stats.samples.getStandardDeviation() / 1000,
                       stats.samples.getMin() / 1000,
                       stats.samples.getPercentile(50) / 1000, 
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