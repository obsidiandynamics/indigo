package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
public interface ThrowingRunnable {
  void run() throws Exception;
  
  static void noOp() {}
}
