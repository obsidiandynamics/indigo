package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class FaultTest implements TestSupport {
  private static final int SCALE = 1;
  
  private static final int DEF_BACKLOG_THROTTLE_CAPACITY = 10;
  
  private static final String SINK = "sink";
  private static final String ECHO = "echo";
  
  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    TestException(String m) { super(m); }
  }
  
  private static ActorSystemConfig system(int actorBias, int actorBacklogThrottleCapacity) {
    return new TestActorSystemConfig() {{
      exceptionHandler = (sys, t) -> {
        if (! (t instanceof TestException)) {
          sys.addError(t);
          t.printStackTrace();
        }
      };
      defaultActorConfig = new ActorConfig() {{
        bias = actorBias;
        backlogThrottleCapacity = actorBacklogThrottleCapacity;
      }};
    }};
  }
  
  @Test
  public void testOnSyncActivationUnbiased() {
    testOnActivation(false, 100 * SCALE, 1);
  }
  
  @Test
  public void testOnSyncActivationBiased() {
    testOnActivation(false, 100 * SCALE, 10);
  }

  @Test
  public void testOnAsyncActivationUnbiased() {
    testOnActivation(true, 100 * SCALE, 1);
  }

  @Test
  public void testOnAsyncActivationBiased() {
    testOnActivation(true, 100 * SCALE, 10);
  }
  
  private void testOnActivation(boolean async, int n, int actorBias) {
    logTestName();
    
    final AtomicInteger activationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedActivations = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system(actorBias, DEF_BACKLOG_THROTTLE_CAPACITY)
    .define()
    .when(SINK)
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           log("activating\n");
           syncOrAsync(a, external, async, () -> {
             if (activationAttempts.getAndIncrement() % 2 == 0) {
               log("fault\n");
               a.fault("boom");
               failedActivations.incrementAndGet();
               
               a.egress(() -> null)
               .using(external)
               .await(1_000).onTimeout(() -> {
                 log("egress timed out\n");
                 fail("egress timed out");
               })
               .onResponse(r -> {
                 log("egress responded\n");
                 fail("egress responded");
               });
               Thread.yield();
             } else {
               log("activated\n");
             }
           });
         })
         .act((a, m) -> {
           log("act %d\n", m.<Integer>body());
           received.getAndIncrement();
           a.passivate();
         })
         .passivated(a -> {
           log("passivated\n");
           passivated.getAndIncrement();
         })
    )
    .ingress().times(n).act((a, i) -> a.to(ActorRef.of(SINK)).tell(i));
    
    try {
      system.drain();
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdown();
    
    log("activationAttempts: %s, failedActivations: %s, received: %s, passivated: %s\n",
        activationAttempts, failedActivations, received, passivated);
    assertTrue(failedActivations.get() >= 1);
    assertTrue(activationAttempts.get() >= failedActivations.get());
    assertTrue(received.get() + failedActivations.get() == n);
    assertTrue(passivated.get() == activationAttempts.get() - failedActivations.get());
  }
  
  @Test
  public void testOnActivationExceptionUnbiased() {
    testOnActivationException(100 * SCALE, 1);
  }
  
  @Test
  public void testOnActivationExceptionBiased() {
    testOnActivationException(100 * SCALE, 10);
  }
  
  private void testOnActivationException(int n, int actorBias) {
    logTestName();
    
    final AtomicInteger activationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedActivations = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system(actorBias, DEF_BACKLOG_THROTTLE_CAPACITY)
    .define()
    .when(SINK)
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           log("activating\n");
           if (activationAttempts.getAndIncrement() % 2 == 0) {
             log("fault\n");
             failedActivations.incrementAndGet();
             
             a.egress(() -> null)
             .using(external)
             .await(1_000).onTimeout(() -> {
               log("egress timed out\n");
               fail("egress timed out");
             })
             .onResponse(r -> {
               log("egress responded\n");
               fail("egress responded");
             });
             Thread.yield();
             throw new TestException("Boom");
           } else {
             log("activated\n");
           }
         })
         .act((a, m) -> {
           log("act %d\n", m.<Integer>body());
           received.getAndIncrement();
           a.passivate();
         })
         .passivated(a -> {
           log("passivated\n");
           passivated.getAndIncrement();
         })
    )
    .ingress().times(n).act((a, i) -> a.to(ActorRef.of(SINK)).tell(i));

    try {
      system.drain();
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdown();
    
    log("activationAttempts: %s, failedActivations: %s, received: %s, passivated: %s\n",
        activationAttempts, failedActivations, received, passivated);
    assertTrue(failedActivations.get() >= 1);
    assertTrue(activationAttempts.get() >= failedActivations.get());
    assertTrue(received.get() + failedActivations.get() == n);
    assertTrue(passivated.get() == activationAttempts.get() - failedActivations.get());
  }
  
  @Test
  public void testRequestResponseUnbiased() {
    testRequestResponse(100 * SCALE, 1);
  }
  
  @Test
  public void testRequestResponseBiased() {
    testRequestResponse(100 * SCALE, 10);
  }
  
  private void testRequestResponse(int n, int actorBias) {
    logTestName();
    
    final AtomicInteger faults = new AtomicInteger();
    final AtomicInteger activationAttempts = new AtomicInteger();
    
    system(actorBias, DEF_BACKLOG_THROTTLE_CAPACITY)
    .define()
    .when(SINK).lambda((a, m) -> {
      log("sink asking\n");

      a.to(ActorRef.of(ECHO)).ask()
      .await(1_000).onTimeout(() -> {
        log("echo timed out\n");
        fail("echo timed out");
      })
      .onFault(f -> {
        log("echo faulted: %s\n", f.getReason());
        faults.getAndIncrement();
      })
      .onResponse(r -> {
        log("echo responded\n");
        fail("echo responded");
      });
    })
    .when(ECHO).configure(new ActorConfig() {{
      // don't exert backpressure on the sink
      backlogThrottleCapacity = Integer.MAX_VALUE;
    }})
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           log("echo activating\n");
           if (activationAttempts.getAndIncrement() % 2 == 0) {
             a.fault("Error in activation");
           }
         })
         .act((a, m) -> {
           log("echo act\n");
           a.fault("Error in act");
           a.passivate();
         })
    )
    .ingress().times(n).act((a, i) -> {
      log("telling sink %d\n", i);
      a.to(ActorRef.of(SINK)).tell(i);
    })
    .shutdown();

    log("activationAttempts: %s, faults: %s\n", activationAttempts, faults);
    assertTrue(activationAttempts.get() >= 1);
    assertEquals(n, faults.get());
  }
  
  @Test
  public void testOnPassivationSyncUnbiased() {
    testOnPassivation(false, 100 * SCALE, 1);
  }
  
  @Test
  public void testOnPassivationSyncBiased() {
    testOnPassivation(false, 100 * SCALE, 10);
  }
  
  @Test
  public void testOnPassivationAsyncUnbiased() {
    testOnPassivation(true, 100 * SCALE, 1);
  }
  
  @Test
  public void testOnPassivationAsyncBiased() {
    testOnPassivation(true, 100 * SCALE, 10);
  }
  
  private void testOnPassivation(boolean async, int n, int actorBias) {
    logTestName();
    
    final AtomicInteger passivationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedPassivations = new AtomicInteger();
    final AtomicBoolean passivationFailed = new AtomicBoolean();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system(actorBias, DEF_BACKLOG_THROTTLE_CAPACITY)
    .define()
    .when(SINK)
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           log("activated\n");
           assertFalse(passivationFailed.get());
         })
         .act((a, m) -> {
           log("act %d\n", m.<Integer>body());
           passivationFailed.set(false);
           received.getAndIncrement();
           a.passivate();
         })
         .passivated(a -> {
           log("passivating\n");
           syncOrAsync(a, external, async, () -> {
             if (passivationAttempts.getAndIncrement() % 2 == 0) {
               log("fault\n");
               a.fault("boom");
               failedPassivations.incrementAndGet();
               passivationFailed.set(true);
               
               a.egress(() -> null)
               .using(external)
               .await(1_000).onTimeout(() -> {
                 log("egress timed out\n");
                 fail("egress timed out");
               })
               .onResponse(r -> {
                 log("egress responded\n");
                 fail("egress responded");
               });
               Thread.yield();
             } else {
               passivated.incrementAndGet();
               log("passivated\n");
             }
           });
         })
    )
    .ingress().times(n).act((a, i) -> a.to(ActorRef.of(SINK)).tell(i));
    
    try {
      system.drain();
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdown();
    
    log("passivationAttempts: %s, failedPassivations: %s, received: %s, passivated: %s\n",
        passivationAttempts, failedPassivations, received, passivated);
    assertTrue(failedPassivations.get() >= 1);
    assertTrue(passivationAttempts.get() >= failedPassivations.get());
    assertTrue(received.get() == n);
    assertTrue(passivated.get() == passivationAttempts.get() - failedPassivations.get());
  }
  
  private void syncOrAsync(Activation a, Executor external, boolean async, Runnable run) {
    if (async) {
      a.egress(() -> null)
      .using(external)
      .await(1_000).onTimeout(() -> {
        log("egress timed out\n");
        fail("egress timed out");
      })
      .onFault(f -> {
        log("egress faulted\n");
        fail("egress faulted");
      })
      .onResponse(r -> {
        run.run();
      });
    } else {
      run.run();
    }
  }
}
