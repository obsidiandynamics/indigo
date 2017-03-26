package com.obsidiandynamics.indigo.benchmark;

interface Spec {
  void init();
  
  LogConfig getLog();
  
  String describe();
  
  Summary run();
  
  default Summary test() {
    final LogConfig log = getLog();
    init();
    if (log.enabled) log.out.println(describe());
    final Summary summary = run();
    if (log.enabled) log.out.format("%s\n", summary);
    return summary;
  }
}
