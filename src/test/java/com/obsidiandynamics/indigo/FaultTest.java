package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import junit.framework.*;

public final class FaultTest implements TestSupport {
  private static final int SCALE = 1;
  
  private static final String SINK = "sink";
  
  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    TestException(String m) { super(m); }
  }
  
  private static ActorSystemConfig system() {
    return new TestActorSystemConfig() {{
      exceptionHandler = (sys, t) -> {
        if (! (t instanceof TestException)) {
          sys.addError(t);
          t.printStackTrace();
        }
      };
      defaultActorConfig = new ActorConfig() {{
        backlogThrottleCapacity = 1;
      }};
    }};
  }
  
  private static void fail(Message m) {
    TestCase.fail();
  }
  
  @Test
  public void testOnSyncActivation() {
    testOnActivation(false, 100 * SCALE);
  }

  @Test
  public void testOnAsyncActivation() {
    testOnActivation(true, 100 * SCALE);
  }
  
  private void testOnActivation(boolean async, int n) {
    logTestName();
    
    final AtomicInteger activationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedActivations = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system()
    .define()
    .when(SINK)
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           if (async) {
             a.egress(() -> null)
             .using(external)
             .await(1_000).onTimeout(TestCase::fail)
             .onResponse(r -> {
               if (activationAttempts.getAndIncrement() % 2 == 0) {
                 log("fault\n");
                 a.fault("boom");
                 failedActivations.incrementAndGet();
                 
                 a.egress(() -> null)
                 .using(external)
                 .await(1_000).onTimeout(TestCase::fail)
                 .onResponse(FaultTest::fail);
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
               .await(1_000).onTimeout(TestCase::fail)
               .onResponse(FaultTest::fail);
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
  public void testOnActivationException() {
    logTestName();
    
    final int n = 100 * SCALE;
    
    final AtomicInteger activationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedActivations = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system()
    .define()
    .when(SINK)
    .use(StatelessLambdaActor.builder()
         .activated(a -> {
           if (activationAttempts.getAndIncrement() % 2 == 0) {
             log("fault\n");
             a.egress(() -> null)
             .using(external)
             .await(1_000).onTimeout(TestCase::fail)
             .onResponse(FaultTest::fail);
             
             failedActivations.incrementAndGet();
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
}
