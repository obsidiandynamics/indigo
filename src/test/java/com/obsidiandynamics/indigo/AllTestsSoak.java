package com.obsidiandynamics.indigo;

import java.util.concurrent.atomic.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

public final class AllTestsSoak {
  public static void main(String[] args) {
    final int cycles = 4;
    final int n = 10;
    final int threads = Runtime.getRuntime().availableProcessors() * 4;

    for (int c = 1; c <= cycles; c++) {
      System.out.format("_\nCycle %d/%d\n", c, cycles);
      test(n, threads, ActorSystemConfig.ExecutorChoice.FIXED_THREAD_POOL, ActorConfig.ActivationChoice.SYNC_QUEUE);
      test(n, threads, ActorSystemConfig.ExecutorChoice.FIXED_THREAD_POOL, ActorConfig.ActivationChoice.NODE_QUEUE);
      test(n, threads, ActorSystemConfig.ExecutorChoice.FORK_JOIN_POOL, ActorConfig.ActivationChoice.SYNC_QUEUE);
      test(n, threads, ActorSystemConfig.ExecutorChoice.FORK_JOIN_POOL, ActorConfig.ActivationChoice.NODE_QUEUE);
    }
  }
  
  private static void test(int n, int threads, ActorSystemConfig.ExecutorChoice executorChoice, ActorConfig.ActivationChoice activationChoice) {
    System.out.format("_\nTesting with [%s, %s]\n", executorChoice, activationChoice);
    System.setProperty(ActorSystemConfig.Key.EXECUTOR, executorChoice.name());
    System.setProperty(ActorConfig.Key.ACTIVATION_FACTORY, activationChoice.name());
    System.setProperty(FaultTest.KEY_TRACE_ENABLED, Boolean.toString(false));
    
    final boolean logFinished = false;
    final boolean logRuns = true;
    
    System.setProperty("indigo.TimeoutTest.timeoutTolerance", String.valueOf(50));
    
    System.out.format("%d parallel runs using %d threads\n", n * threads, threads);
    final AtomicLong totalTests = new AtomicLong();
    final long took = TestSupport.took(() -> {
      for (int i = 1; i <= n; i++) {
        ParallelJob.blocking(threads, t -> {
          final Computer computer = new Computer();
          final JUnitCore core = new JUnitCore();
          core.addListener(new RunListener() {
            @Override public void testFinished(Description description) throws Exception {
              if (logFinished) System.out.println("Finished: " + description);
              totalTests.incrementAndGet();
            }
            
            @Override public void testFailure(Failure failure) throws Exception {
              System.err.println("Failed: " + failure);
            }
          });
          core.run(computer, AllTests.class);
        }).run();
        
        if (logRuns) {
          System.out.format("Finished run %,d: %,d active threads, free mem: %,.0f MB\n", 
                            i, Thread.activeCount(), Runtime.getRuntime().freeMemory() / Math.pow(2, 20));
        }
      }
    });
    System.out.format("Complete: %,d tests took %d s, %.1f tests/s\n", 
                      totalTests.get(), took / 1000, totalTests.get() * 1000f / took);
  }
}
