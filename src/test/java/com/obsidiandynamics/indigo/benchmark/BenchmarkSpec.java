package com.obsidiandynamics.indigo.benchmark;

import com.obsidiandynamics.indigo.benchmark.BenchmarkSupport.*;

interface BenchmarkSpec {
  void init();
  
  LogConfig log();
  
  String describe();
  
  Timings run();
  
  default Timings test() {
    final LogConfig log = log();
    init();
    if (log.enabled) log.out.println(describe());
    final Timings t = run();
    if (log.enabled) log.out.format("%s\n", t);
    return t;
  }
}
