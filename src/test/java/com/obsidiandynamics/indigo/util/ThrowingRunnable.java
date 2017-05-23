package com.obsidiandynamics.indigo.util;

@FunctionalInterface
public interface ThrowingRunnable {
  void run() throws Exception;
  
  static void noOp() {}
}
