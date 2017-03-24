package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

public final class AllTestsLoop {
  public static void main(String[] args) {
    final int n = 20;
    final int threads = Runtime.getRuntime().availableProcessors() * 2;
    final boolean logFinished = false;
    
    final RunListener listener = new RunListener() {
      @Override public void testFinished(Description description) throws Exception {
        if (logFinished) System.out.println("Finished: " + description);
      }
      
      @Override public void testFailure(Failure failure) throws Exception {
        System.err.println("Failed: " + failure);
      }
    };
    
    System.out.format("Running %d parallel tests using %d threads\n", n * threads, threads);
    final long now = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      final CountDownLatch latch = new CountDownLatch(threads);
      ParallelJob.create(threads, latch, t -> {
        final Computer computer = new Computer();
        final JUnitCore core = new JUnitCore();
        core.addListener(listener);
        core.run(computer, AllTests.class);
        latch.countDown();
      }).run();
    }
    final long took = System.currentTimeMillis() - now;
    System.out.format("Complete: took %d s, %.1f tests/s\n", took / 1000, n * threads * 1000f / took);
  }
}
