package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class FaultTest implements TestSupport {
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
           if (async) {
             a.egress(() -> null)
             .using(external)
             .await(1_000).onTimeout(() -> {
               log("egress timed out\n");
               fail("egress timed out");
             })
             .onResponse(r -> {
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
                 .onResponse(r2 -> {
                   log("egress responded\n");
                   fail("egress responded");
                 });
                 Thread.yield();
               } else {
                 log("activated\n");
               }
             });
           } else {
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
    
    system(actorBias)
    .define()
    .when(SINK).lambda((a, m) -> {
      log("sink asking\n");
      a.to(ActorRef.of(ECHO)).ask()
      .await(1_000).onTimeout(() -> {
        log("echo timed out\n");
        fail("echo timed out");
      })
      .onFault(f -> {
        log("echo faulted\n");
        faults.getAndIncrement();
      })
      .onResponse(r -> {
        log("echo responded\n");
        fail("echo responded");
      });
    })
    .when(ECHO).lambda((a, m) -> {
      a.fault("Some error");
    })
    .ingress().times(n).act((a, i) -> a.to(ActorRef.of(SINK)).tell(i))
    .shutdown();
    
    assertEquals(n, faults.get());
  }
}
