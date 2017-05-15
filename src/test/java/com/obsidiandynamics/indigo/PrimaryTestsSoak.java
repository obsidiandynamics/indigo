package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.util.OneShotContract.*;

public final class PrimaryTestsSoak {
  public static void main(String[] args) throws InterruptedException, ExecutionException {
    if (! Assertions.areEnabled()) {
      System.err.println("Assertions need to be enabled for tracing, run JVM with -ea flag");
      System.exit(1);
    }
    
    final String ONE_SHOT = "one_shot";
    final ActorSystem system = new ActorSystemConfig() {}
    .createActorSystem()
    .useExecutor(TestSupport.oneTimeExecutor("SoakRunner")).named("runner")
    .on(ONE_SHOT).cue(() -> new OneShotActor((a, b) -> {
      final CompletableFuture<?> f = new CompletableFuture<>();
      
      a.egress(PrimaryTestsSoak::runForMinutes)
      .withExecutor("runner")
      .ask(b)
      .onResponse(r -> f.complete(r.body()));
      
      return f;
    }));
    
    final int DEF_MINS = 10;
    final int DEF_TIMEOUT_MILLIS = 5_000;
    TestSupport.asyncDaemon(() -> {
      System.out.format("Number of minutes to soak [10]: ");
      int mins;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
        final String read = reader.readLine();
        if (read == null || read.trim().isEmpty()) {
          mins = DEF_MINS;
        } else {
          try {
            mins = Integer.parseInt(read);
          } catch (NumberFormatException e) {
            System.out.format("Couldn't parse '%s', using default\n", read);
            mins = DEF_MINS;
          }
        }
      } catch (IOException e) {
        System.out.format("Error reading stdin, using default\n");
        mins = DEF_MINS;
      }
      system.tell(ActorRef.of(ONE_SHOT), new Fire(mins));
    }, "StdinReader");
    
    TestSupport.asyncDaemon(() -> {
      TestSupport.sleep(DEF_TIMEOUT_MILLIS);
      system.tell(ActorRef.of(ONE_SHOT), new Fire(DEF_MINS));
    }, "DefaultTimeout");
    
    while (! system.<GetStatusResponse>ask(ActorRef.of(ONE_SHOT), new GetStatus()).get().getStatus().isFinished()) {
      TestSupport.sleep(100);
    }
    
    system.shutdownQuietly();
    System.out.format("Soak completed\n");
  }
  
  private static void runForMinutes(int minutesMinimum) {
    System.out.format("\nSoaking for at least %d minute(s)...\n", minutesMinimum);
    final int n = 10;
    final int threads = Runtime.getRuntime().availableProcessors() * 2;
    final long end = System.currentTimeMillis() + minutesMinimum * 60_000;
    int cycle = 1;
    while (System.currentTimeMillis() < end) {
      System.out.format("_\nCycle %d\n", cycle++);
      test(n, threads, ActorSystemConfig.ExecutorChoice.FIXED_THREAD_POOL, ActorConfig.ActivationChoice.NODE_QUEUE);
      test(n, threads, ActorSystemConfig.ExecutorChoice.AUTO, ActorConfig.ActivationChoice.SYNC_QUEUE);
      test(n, threads, ActorSystemConfig.ExecutorChoice.FORK_JOIN_POOL, ActorConfig.ActivationChoice.NODE_QUEUE);
    }
  }
  
  private static void test(int n, int threads, ActorSystemConfig.ExecutorChoice executorChoice, ActorConfig.ActivationChoice activationChoice) {
    System.out.format("_\nTesting with [%s, %s]\n", executorChoice, activationChoice);
    System.setProperty(ActorSystemConfig.Key.EXECUTOR, executorChoice.name());
    System.setProperty(ActorConfig.Key.ACTIVATION_FACTORY, activationChoice.name());
    System.setProperty(FaultTest.KEY_TRACE_ENABLED, Boolean.toString(false));
    System.setProperty(TimeoutTest.KEY_TIMEOUT_TOLERANCE, String.valueOf(1_000));
    
    final boolean logFinished = false;
    final boolean logRuns = true;
    
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
              System.err.println(failure.getTrace());
            }
          });
          core.run(computer, PrimaryTests.class);
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
