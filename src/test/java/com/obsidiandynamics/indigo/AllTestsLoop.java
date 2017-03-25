package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runner.notification.*;

public final class AllTestsLoop {
  public static void main(String[] args) {
    final int n = 200;
    final int threads = Runtime.getRuntime().availableProcessors() * 2;
    final boolean logFinished = false;
    final boolean logActiveThreads = false;
    
    final RunListener listener = new RunListener() {
      @Override public void testFinished(Description description) throws Exception {
        if (logFinished) System.out.println("Finished: " + description);
      }
      
      @Override public void testFailure(Failure failure) throws Exception {
        System.err.println("Failed: " + failure);
      }
    };
    
    System.setProperty("indigo.TimeoutTest.timeoutTolerance", String.valueOf(50));
    
    System.out.format("Running %d parallel tests using %d threads\n", n * threads, threads);
    final long took = TestSupport.took(() -> {
      for (int i = 0; i < n; i++) {
        ParallelJob.blocking(threads, t -> {
          final Computer computer = new Computer();
          final JUnitCore core = new JUnitCore();
          core.addListener(listener);
          core.run(computer, AllTests.class);
        }).run();
        if (logActiveThreads) System.out.format("%d active threads\n", Thread.activeCount());
      }
    });
    System.out.format("Complete: took %d s, %.1f tests/s\n", took / 1000, n * threads * 1000f / took);
  }
}
