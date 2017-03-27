package com.obsidiandynamics.indigo.benchmark;

import java.util.*;

interface Spec {
  void init();
  
  LogConfig getLog();
  
  String describe();
  
  Summary run();
  
  default Summary test() {
    final LogConfig log = getLog();
    init();
    if (log.summary) log.out.println(describe());
    final Summary summary = run();
    if (log.summary) log.out.format("%s\n", summary);
    return summary;
  }
  
  default Summary testPercentile(int discard, int keep, double percentile, Comparator<? super Summary> comparator) {
    init();
    for (int i = 0; i < discard; i++) {
      run();
    }
    BenchmarkSupport.forceGC();
    
    final List<Summary> summaries = new ArrayList<>(keep);
    for (int i = 0; i < keep; i++) {
      final Summary summary = run();
      summaries.add(summary);
      BenchmarkSupport.forceGC();
    }
    
    Collections.sort(summaries, comparator);
    
    final LogConfig log = getLog();
    final Summary selected = summaries.get((int) Math.round(percentile * (keep - 1) / 100));
    if (log.summary) {
      log.out.println(describe());
      log.out.format("%s\n", selected);
    }
    return selected;
  }
}
