package com.obsidiandynamics.indigo.benchmark;

import java.util.*;

import com.obsidiandynamics.func.*;

public interface Spec {
  void init();
  
  LogConfig getLog();
  
  String describe();
  
  Summary run() throws Exception;
  
  final class SpecMultiplier {
    private final Spec spec;
    private final int times;
    private CheckedRunnable<Throwable> onFinally = () -> {};

    SpecMultiplier(Spec spec, int times) {
      this.spec = spec;
      this.times = times;
    }
    
    public SpecMultiplier andFinally(CheckedRunnable<Throwable> onFinally) {
      this.onFinally = onFinally;
      return this;
    }
    
    public List<Summary> test() throws Throwable {
      final List<Summary> summaries = new ArrayList<>(times);
      try {
        for (int i = 0; i < times; i++) {
          summaries.add(spec.test());
        }
      } finally {
        onFinally.run();
      }
      return summaries;
    }
  }
  
  default SpecMultiplier times(int times) {
    return new SpecMultiplier(this, times);
  }
  
  default Summary test() throws Exception {
    final LogConfig log = getLog();
    init();
    if (log.summary) log.out.println(describe());
    final Summary summary = run();
    if (log.summary) log.out.format("%s\n", summary);
    return summary;
  }
  
  default Summary testPercentile(int discard, int keep, double percentile, Comparator<? super Summary> comparator) throws Exception {
    final LogConfig log = getLog();
    init();
    
    final int digits = String.valueOf(discard + keep).length();
    for (int i = 0; i < discard; i++) {
      if (log.progress) log.out.print(printRunNo(digits, i + 1) + " > ");
      final Summary summary = run();
      if (log.progress) log.out.println();
      if (log.intermediateSummaries) log.out.println(summary);
    }
    BenchmarkSupport.forceGC();
    
    final List<Summary> summaries = new ArrayList<>(keep);
    for (int i = 0; i < keep; i++) {
      if (log.progress) log.out.print(printRunNo(digits, i + discard + 1) + " > ");
      final Summary summary = run();
      summaries.add(summary);
      BenchmarkSupport.forceGC();
      if (log.progress) log.out.println();
      if (log.intermediateSummaries) log.out.println(summary);
    }
    
    summaries.sort(comparator);
    
    final Summary selected = summaries.get((int) Math.round(percentile * (keep - 1) / 100));
    if (log.summary) {
      if (log.progress || log.intermediateSummaries) log.out.println("_");
      log.out.println(describe());
      log.out.println(selected);
    }
    return selected;
  }
  
  static String printRunNo(int digits, int runNo) {
    return String.format("%,0" + digits + "d", runNo);
  }
}
