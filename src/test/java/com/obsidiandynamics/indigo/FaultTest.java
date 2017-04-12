package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.FaultType.*;
import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class FaultTest implements TestSupport {
  public static final String KEY_TRACE_ENABLED = "indigo.FaultTest.traceEnabled";
  
  private static final int SCALE = 1;
  
  private static final String SINK = "sink";
  private static final String ECHO = "echo";
  
  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    TestException(String m) { super(m); }
  }
  
  private static ActorSystemConfig system(int actorBias) {
    return new TestActorSystemConfig() {{
      exceptionHandler = (sys, t) -> {
        if (! (t instanceof TestException)) {
          sys.addError(t);
          t.printStackTrace();
        }
      };
      
      diagnostics = new Diagnostics() {{
        traceEnabled = get(KEY_TRACE_ENABLED, Boolean::parseBoolean, true);
      }};
      
      defaultActorConfig = new ActorConfig() {{
        bias = actorBias;
        backlogThrottleCapacity = 10;
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

    final ActorSystem system = system(actorBias)
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
      system.drain(0);
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdownQuietly();
    
    log("activationAttempts: %s, failedActivations: %s, received: %s, passivated: %s\n",
        activationAttempts, failedActivations, received, passivated);
    assertTrue(failedActivations.get() >= 1);
    assertTrue(activationAttempts.get() >= failedActivations.get());
    assertTrue(received.get() + failedActivations.get() == n);
    assertTrue(passivated.get() == activationAttempts.get() - failedActivations.get());
    if (async) {
      assertTrue(failedActivations.get() * 2 == system.getDeadLetterQueue().size());
      assertTrue(failedActivations.get() == count(ON_ACTIVATION, system.getDeadLetterQueue()));
      assertTrue(failedActivations.get() == count(ON_RESPONSE, system.getDeadLetterQueue()));
    } else {
      assertTrue(failedActivations.get() == system.getDeadLetterQueue().size());
      assertTrue(failedActivations.get() == count(ON_ACTIVATION, system.getDeadLetterQueue()));
    }
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

    final ActorSystem system = system(actorBias)
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
      system.drain(0);
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdownQuietly();
    
    log("activationAttempts: %s, failedActivations: %s, received: %s, passivated: %s\n",
        activationAttempts, failedActivations, received, passivated);
    assertTrue(failedActivations.get() >= 1);
    assertTrue(activationAttempts.get() >= failedActivations.get());
    assertTrue(received.get() + failedActivations.get() == n);
    assertTrue(passivated.get() == activationAttempts.get() - failedActivations.get());
    assertTrue(failedActivations.get() == system.getDeadLetterQueue().size());
    assertTrue(failedActivations.get() == count(ON_ACTIVATION, system.getDeadLetterQueue()));
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
    
    final ActorSystem system = system(actorBias)
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
    .when(ECHO)
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
    });
    system.shutdownQuietly();

    log("activationAttempts: %s, faults: %s\n", activationAttempts, faults);
    assertTrue(activationAttempts.get() >= 1);
    assertEquals(n, faults.get());
    assertTrue(faults.get() == system.getDeadLetterQueue().size());
    assertTrue(count(ON_ACTIVATION, system.getDeadLetterQueue()) + count(ON_ACT, system.getDeadLetterQueue()) == faults.get());
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

    final ActorSystem system = system(actorBias)
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
      system.drain(0);
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdownQuietly();
    
    log("passivationAttempts: %s, failedPassivations: %s, received: %s, passivated: %s\n",
        passivationAttempts, failedPassivations, received, passivated);
    assertTrue(failedPassivations.get() >= 1);
    assertTrue(passivationAttempts.get() >= failedPassivations.get());
    assertEquals(n, received.get());
    assertTrue(passivated.get() == passivationAttempts.get() - failedPassivations.get());
    if (async) {
      assertTrue(failedPassivations.get() * 2 == system.getDeadLetterQueue().size());
      assertEquals(failedPassivations.get(), count(ON_PASSIVATION, system.getDeadLetterQueue()));
      assertEquals(failedPassivations.get(), count(ON_RESPONSE, system.getDeadLetterQueue()));
    } else {
      assertTrue(failedPassivations.get() == system.getDeadLetterQueue().size());
      assertEquals(failedPassivations.get(), count(ON_PASSIVATION, system.getDeadLetterQueue()));
    }
  }
  
  @Test
  public void testOnEgressBiased() {
    testOnEgress(100 * SCALE, 10);
  }
  
  private void testOnEgress(int n, int actorBias) {
    logTestName();
    
    final AtomicInteger faults = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();
    
    final ActorSystem system = system(actorBias)
    .define()
    .ingress().times(n).act((a, i) -> {
      final Diagnostics d = a.diagnostics();
      d.trace("act %d", i);
      a.egress(() -> {
        d.trace("egress %d", i);
        throw new TestException("Fault in egress");
      })
      .using(external)
      .await(1_000).onTimeout(() -> {
        log("egress timed out\n");
        fail("egress timed out");
      })
      .onFault(f -> {
        d.trace("fault %d", i);
        faults.incrementAndGet();
      })
      .onResponse(r -> {
        log("egress responded\n");
        fail("egress responded");
      });
    });
    
    try {
      for (long left; (left = system.drain(10_000)) != 0; ) {
        log("draining... faults: %s, actors left: %d\n", faults, left);
        system.getConfig().diagnostics.print(LOG_STREAM);
        fail("drain did not complete");
      }
      external.shutdown();
      external.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) { throw new RuntimeException(e); }
    system.shutdownQuietly();
    
    assertEquals(n, faults.get());
    assertEquals(n, system.getDeadLetterQueue().size());
    assertEquals(n, count(ON_EGRESS, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testOnFault() {
    testOnFault(100 * SCALE, 10);
  }
  
  private void testOnFault(int n, int actorBias) {
    logTestName();
    
    final ActorSystem system = system(actorBias)
    .define()
    .when(SINK).lambda((a, m) -> {
      log("sink act %d\n", m.<Integer>body());
      a.fault("fault in act");
    })
    .ingress().times(n).act((a, i) -> {
      log("asking sink %d\n", i);
      a.to(ActorRef.of(SINK)).ask(i)
      .await(1_000).onTimeout(() -> {
        log("sink timed out\n");
        fail("sink timed out");
      })
      .onFault(f -> {
        a.fault("fault in onFault");
      })
      .onResponse(r -> {
        log("sink responded\n");
        fail("sink responded");
      });
    });
    
    system.shutdownQuietly();
    assertEquals(n * 2, system.getDeadLetterQueue().size());
    assertEquals(n, count(ON_ACT, system.getDeadLetterQueue()));
    assertEquals(n, count(ON_FAULT, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testOnTimeout() {
    testOnTimeout(100 * SCALE, 10);
  }
  
  private void testOnTimeout(int n, int actorBias) {
    logTestName();
    
    final ActorSystem system = system(actorBias)
    .define()
    .when(SINK).lambda((a, m) -> { /* stall the response */ })
    .ingress().times(n).act((a, i) -> {
      a.to(ActorRef.of(SINK)).ask()
      .await(1).onTimeout(() -> {
        a.fault("fault in onTimeout");
      })
      .onFault(f -> {
        log("sink faulted\n");
        fail("sink faulted");
      })
      .onResponse(r -> {
        log("sink responded\n");
        fail("sink responded");
      });
    });
    
    system.shutdownQuietly();
    assertEquals(n, system.getDeadLetterQueue().size());
    assertEquals(n, count(ON_TIMEOUT, system.getDeadLetterQueue()));
  }
  
  private static int count(FaultType type, Queue<Fault> deadLetterQueue) {
    int count = 0;
    for (Fault fault : deadLetterQueue) {
      if (fault.getType() == type) {
        count++;
      }
    }
    return count;
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
