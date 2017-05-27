package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.FaultType.*;
import static com.obsidiandynamics.indigo.util.TestSupport.*;
import static com.obsidiandynamics.indigo.util.PropertyUtils.*;
import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class FaultTest implements TestSupport {
  public static final String KEY_TRACE_ENABLED = "indigo.FaultTest.traceEnabled";
  
  private static final int SCALE = 1;
  
  private static final String SINK = "sink";
  private static final String ECHO = "echo";
  
  private static ActorSystemConfig system(int actorBias) {
    return new TestActorSystemConfig() {{
      exceptionHandler = TestException.BYPASS_DRAIN_HANDLER;
      deadLetterQueueSize = Integer.MAX_VALUE;
      
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
  public void testSimpleAsyncActivation() {
    final AtomicBoolean faulted = new AtomicBoolean();
    final AtomicBoolean handled = new AtomicBoolean();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();
    final ActorSystem system = system(1)
    .createActorSystem()
    .addExecutor(external).named("ext")
    .on(SINK)
    .cue(StatelessLambdaActor.builder()
         .activated(a -> {
           log("activating\n");
           a.egress(() -> null)
           .withExecutor("ext")
           .await(60_000).onTimeout(() -> {
             log("egress timed out\n");
             fail("egress timed out");
           })
           .onFault(f -> {
             log("egress faulted\n");
             fail("egress faulted");
           })
           .onResponse(r -> {
             a.fault("boom");
             faulted.set(true);
           });
         })
         .act((a, m) -> {
           log("act\n");
           a.passivate();
         })
         .passivated(a -> {
           log("passivated\n");
         })
    )
    .ingress(a -> { 
      a.to(ActorRef.of(SINK)).ask()
      .onFault(f -> {
        log("fault detected");
        handled.set(true);
      })
      .onResponse(r -> {
        fail("Unexpected response\n");
      });
    });
    system.shutdownQuietly();
    external.shutdown();
    
    assertTrue(faulted.get());
    assertTrue(handled.get());
    assertEquals(2, system.getDeadLetterQueue().size());
    assertEquals(1, countFaults(ON_RESPONSE, system.getDeadLetterQueue()));
    assertEquals(1, countFaults(ON_ACTIVATION, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testPropagationOnActivation() {
    final AtomicBoolean propagated = new AtomicBoolean();
    final ActorSystem system = system(1)
    .createActorSystem()
    .on(SINK)
    .cue(StatelessLambdaActor.builder()
         .activated(a -> {
           log("activating\n");
           a.egress(() -> {
             throw new TestException("test fault"); 
           })
           .onFault(a::propagateFault)
           .onResponse(r -> {
             fail("Unexpected response\n");
           });
         })
         .act((a, m) -> {
           fail("Unexpected act\n");
         })
         .passivated(a -> {
           fail("Unexpected passivation\n");
         })
    )
    .ingress(a -> { 
      a.to(ActorRef.of(SINK)).ask()
      .onFault(f -> {
        log("fault detected");
        propagated.set(true);
        assertNotNull(f.getReason());
        assertEquals(TestException.class, f.getReason().getClass());
      })
      .onResponse(r -> {
        fail("Unexpected response\n");
      });
    });
    system.shutdownQuietly();
    
    assertTrue(propagated.get());
  }
  
  @Test
  public void testOnSyncActivationUnbiased() {
    testOnActivation(attempts -> false, 100 * SCALE, 1, false);
  }
  
  @Test
  public void testOnSyncActivationBiased() {
    testOnActivation(attempts -> false, 100 * SCALE, 10, false);
  }

  @Test
  public void testOnAsyncActivationUnbiased() {
    testOnActivation(attempts -> true, 100 * SCALE, 1, false);
  }

  @Test
  public void testOnAsyncActivationBiased() {
    testOnActivation(attempts -> true, 100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnMixedActivationUnbiased() {
    testOnActivation(attempts -> attempts % 2 == 0, 100 * SCALE, 1, false);
  }
  
  @Test
  public void testOnMixedActivationBiased() {
    testOnActivation(attempts -> attempts % 2 == 0, 100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnSyncActivationBiasedException() {
    testOnActivation(attempts -> false, 100 * SCALE, 10, true);
  }
  
  @Test
  public void testOnAsyncActivationBiasedException() {
    testOnActivation(attempts -> true, 100 * SCALE, 10, true);
  }
  
  private void testOnActivation(Function<Integer, Boolean> asyncTest, int n, int actorBias, boolean exception) {
    final AtomicInteger activationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedAsyncActivations = new AtomicInteger();
    final AtomicInteger failedSyncActivations = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .addExecutor(external).named("ext")
    .on(SINK)
    .cue(StatelessLambdaActor.builder()
         .activated(a -> {
           log("activating\n");
           final boolean async = asyncTest.apply(activationAttempts.get());
           syncOrAsync(a, external, async, () -> {
             if (activationAttempts.incrementAndGet() % 10 != 0) {
               log("fault\n");
               
               if (async) {
                 failedAsyncActivations.incrementAndGet();
               } else {
                 failedSyncActivations.incrementAndGet();
               }
               
               a.egress(() -> null)
               .withExecutor("ext")
               .await(1).onTimeout(() -> {
                 log("egress timed out\n");
                 fail("egress timed out");
               })
               .onResponse(r -> {
                 log("egress responded\n");
                 fail("egress responded");
               });
               
               if (exception) throw new TestException("boom");
               else a.fault("boom");
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
    
    final int failedActivations = failedAsyncActivations.get() + failedSyncActivations.get();
    log("activationAttempts: %s, failedAsyncActivations: %s, failedSyncActivations: %s, received: %s, passivated: %s\n",
        activationAttempts, failedAsyncActivations, failedSyncActivations, received, passivated);
    assertTrue("failedActivations=" + failedActivations, 
               failedActivations >= 1);
    assertTrue("activationAttempts=" + activationAttempts + ", failedActivations=" + failedActivations, 
               activationAttempts.get() >= failedActivations);
    assertTrue("received=" + received + ", failedActivations=" + failedActivations + ", n=" + n, 
               received.get() + failedActivations == n);
    assertTrue("passivated=" + passivated + ", activationAttempts=" + activationAttempts + ", failedActivations=" + failedActivations,
               passivated.get() == activationAttempts.get() - failedActivations);
    assertTrue("failedAsyncActivations=" + failedAsyncActivations + ", failedSyncActivations=" + failedSyncActivations + ", dlq.size=" + system.getDeadLetterQueue().size(), 
               failedAsyncActivations.get() * 2 + failedSyncActivations.get() == system.getDeadLetterQueue().size());
    final int activationFaults = countFaults(ON_ACTIVATION, system.getDeadLetterQueue());
    assertTrue("failedAsyncActivations=" + failedAsyncActivations + ", failedSyncActivations" + failedSyncActivations + ", activationFaults=" + activationFaults,
               failedAsyncActivations.get() + failedSyncActivations.get() == activationFaults);
    final int responseFaults = countFaults(ON_RESPONSE, system.getDeadLetterQueue());
    assertTrue("failedAsyncActivations=" + failedAsyncActivations + ", responseFaults=" + responseFaults,
               failedAsyncActivations.get() == responseFaults);
  }
  
  @Test
  public void testRequestResponseUnbiased() {
    testRequestResponse(100 * SCALE, 1, false);
  }
  
  @Test
  public void testRequestResponseBiased() {
    testRequestResponse(100 * SCALE, 10, false);
  }
  
  @Test
  public void testRequestResponseBiasedException() {
    testRequestResponse(100 * SCALE, 10, true);
  }
  
  private void testRequestResponse(int n, int actorBias, boolean exception) {
    final AtomicInteger faults = new AtomicInteger();
    final AtomicInteger activationAttempts = new AtomicInteger();
    
    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .on(SINK).cue((a, m) -> {
      log("sink asking\n");

      a.to(ActorRef.of(ECHO)).ask()
      .await(60_000).onTimeout(() -> {
        log("echo timed out\n");
        fail("echo timed out");
      })
      .onFault(f -> {
        assertNotNull(f.getOriginalMessage());
        log("echo faulted: %s\n", f.<Object>getReason());
        faults.getAndIncrement();
      })
      .onResponse(r -> {
        log("echo responded\n");
        fail("echo responded");
      });
    })
    .on(ECHO)
    .cue(StatelessLambdaActor.builder()
         .activated(a -> {
           log("echo activating\n");
           if (activationAttempts.getAndIncrement() % 2 == 0) {
             if (exception) throw new TestException("Error in activation");
             else a.fault("Error in activation");
           }
         })
         .act((a, m) -> {
           log("echo act\n");
           if (exception) throw new TestException("Error in act");
           else a.fault("Error in act");
           a.passivate();
         })
    )
    .ingress().times(n).act((a, i) -> {
      log("telling sink %d\n", i);
      a.to(ActorRef.of(SINK)).tell(i);
    });
    system.shutdownQuietly();

    log("activationAttempts: %s, faults: %s\n", activationAttempts, faults);
    assertTrue("activationAttempts=" + activationAttempts, activationAttempts.get() >= 1);
    assertEquals(n, faults.get());
    assertTrue("faults=" + faults + ", dlq.size=" + system.getDeadLetterQueue().size(),
               faults.get() == system.getDeadLetterQueue().size());
    final int activationFaults = countFaults(ON_ACTIVATION, system.getDeadLetterQueue());
    final int actFaults = countFaults(ON_ACT, system.getDeadLetterQueue());
    assertTrue("activationFaults=" + activationFaults + ", actFaults=" + actFaults + ", faults=" + faults,
               activationFaults + actFaults == faults.get());
  }
  
  @Test
  public void testOnSyncPassivationUnbiased() {
    testOnPassivation(false, 100 * SCALE, 1, false);
  }
  
  @Test
  public void testOnSyncPassivationBiased() {
    testOnPassivation(false, 100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnAsyncPassivationUnbiased() {
    testOnPassivation(true, 100 * SCALE, 1, false);
  }
  
  @Test
  public void testOnAsyncPassivationBiased() {
    testOnPassivation(true, 100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnSyncPassivationBiasedException() {
    testOnPassivation(false, 100 * SCALE, 10, true);
  }
  
  private void testOnPassivation(boolean async, int n, int actorBias, boolean exception) {
    final AtomicInteger passivationAttempts = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger failedPassivations = new AtomicInteger();
    final AtomicBoolean passivationFailed = new AtomicBoolean();
    final AtomicInteger passivated = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();

    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .addExecutor(external).named("ext")
    .on(SINK)
    .cue(StatelessLambdaActor.builder()
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
               failedPassivations.incrementAndGet();
               passivationFailed.set(true);
               
               a.egress(() -> null)
               .withExecutor("ext")
               .await(60_000).onTimeout(() -> {
                 log("egress timed out\n");
                 fail("egress timed out");
               })
               .onResponse(r -> {
                 log("egress responded\n");
                 fail("egress responded");
               });
               
               Thread.yield();
               if (exception) throw new TestException("boom");
               else a.fault("boom");
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
    assertTrue("failedPassivations=" + failedPassivations, failedPassivations.get() >= 1);
    assertTrue("passivationAttempts=" + passivationAttempts + ", failedPassivations=" + failedPassivations,
               passivationAttempts.get() >= failedPassivations.get());
    assertEquals(n, received.get());
    assertTrue("passivated=" + passivated + ", passivationAttempts=" + passivationAttempts + ", failedPassivations=" + failedPassivations,
               passivated.get() == passivationAttempts.get() - failedPassivations.get());
    final int dlqSize = system.getDeadLetterQueue().size();
    if (async) {
      assertTrue("failedPassivations=" + failedPassivations + ", dlqSize=" + dlqSize,
                 failedPassivations.get() * 2 == dlqSize);
      assertEquals(failedPassivations.get(), countFaults(ON_PASSIVATION, system.getDeadLetterQueue()));
      assertEquals(failedPassivations.get(), countFaults(ON_RESPONSE, system.getDeadLetterQueue()));
    } else {
      assertTrue("failedPassivations=" + failedPassivations + ", dlqSize=" + dlqSize,
                 failedPassivations.get() == dlqSize);
      assertEquals(failedPassivations.get(), countFaults(ON_PASSIVATION, system.getDeadLetterQueue()));
    }
  }
  
  @Test
  public void testOnSerialEgressAskBiased() {
    testOnEgressAsk(100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnParallelEgressAskBiased() {
    testOnEgressAsk(100 * SCALE, 10, true);
  }
  
  private void testOnEgressAsk(int n, int actorBias, boolean parallel) {
    final AtomicInteger faults = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();
    
    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .addExecutor(external).named("ext")
    .ingress().times(n).act((a, i) -> {
      final Diagnostics d = a.diagnostics();
      d.trace("act %d", i);
      egressMode(a.egress(() -> {
        d.trace("egress %d", i);
        throw new TestException("Fault in egress");
      }), parallel)
      .withExecutor("ext")
      .await(60_000).onTimeout(() -> {
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
    assertEquals(n, countFaults(ON_EGRESS, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testOnAsyncSerialEgressBiased() {
    testOnAsyncEgressAsk(100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnAsyncParallelEgressBiased() {
    testOnAsyncEgressAsk(100 * SCALE, 10, true);
  }
  
  private void testOnAsyncEgressAsk(int n, int actorBias, boolean parallel) {
    final AtomicInteger faults = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();
    
    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .addExecutor(external).named("ext")
    .ingress().times(n).act((a, i) -> {
      final Diagnostics d = a.diagnostics();
      d.trace("act %d", i);
      egressMode(a.egressAsync(() -> {
        d.trace("egress %d", i);
        final CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new TestException("Fault in async egress"));
        return future;
      }), parallel)
      .withExecutor("ext")
      .await(60_000).onTimeout(() -> {
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
    assertEquals(n, countFaults(ON_EGRESS, system.getDeadLetterQueue()));
  }

  @Test
  public void testOnEgressTellBiased() {
    testOnEgressTell(100 * SCALE, 10);
  }
  
  private void testOnEgressTell(int n, int actorBias) {
    final AtomicInteger faults = new AtomicInteger();
    
    final ExecutorService external = Executors.newSingleThreadExecutor();
    
    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .addExecutor(external).named("ext")
    .ingress().times(n).act((a, i) -> {
      final Diagnostics d = a.diagnostics();
      d.trace("act %d", i);
      a.egress(() -> {
        d.trace("egress %d", i);
        faults.incrementAndGet();
        throw new TestException("Fault in egress");
      })
      .withExecutor("ext")
      .tell();
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
    assertEquals(n, countFaults(ON_EGRESS, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testOnFaultBiased() {
    testOnFault(100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnFaultBiasedException() {
    testOnFault(100 * SCALE, 10, true);
  }
  
  private void testOnFault(int n, int actorBias, boolean exception) {
    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .on(SINK).cue((a, m) -> {
      log("sink act %d\n", m.<Integer>body());
      if (exception) throw new TestException("fault in act");
      else a.fault("fault in act");
    })
    .ingress().times(n).act((a, i) -> {
      log("asking sink %d\n", i);
      a.to(ActorRef.of(SINK)).ask(i)
      .await(60_000).onTimeout(() -> {
        log("sink timed out\n");
        fail("sink timed out");
      })
      .onFault(f -> {
        if (exception) throw new TestException("fault in onFault");
        else a.fault("fault in onFault");
      })
      .onResponse(r -> {
        log("sink responded\n");
        fail("sink responded");
      });
    });
    
    system.shutdownQuietly();
    assertEquals(n * 2, system.getDeadLetterQueue().size());
    assertEquals(n, countFaults(ON_ACT, system.getDeadLetterQueue()));
    assertEquals(n, countFaults(ON_FAULT, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testOnTimeoutBiased() {
    testOnTimeout(100 * SCALE, 10, false);
  }
  
  @Test
  public void testOnTimeoutBiasedException() {
    testOnTimeout(100 * SCALE, 10, true);
  }
  
  private void testOnTimeout(int n, int actorBias, boolean exception) {
    final ActorSystem system = system(actorBias)
    .createActorSystem()
    .on(SINK).cue((a, m) -> { /* stall the response */ })
    .ingress().times(n).act((a, i) -> {
      a.to(ActorRef.of(SINK)).ask()
      .await(1).onTimeout(() -> {
        if (exception) throw new TestException("fault in onTimeout");
        else a.fault("fault in onTimeout");
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
    assertEquals(n, countFaults(ON_TIMEOUT, system.getDeadLetterQueue()));
  }
  
  @Test
  public void testOnExternal() {
    final AtomicBoolean faulted = new AtomicBoolean();
    
    final ActorSystem system = system(1)
    .createActorSystem()
    .on(SINK).cue((a, m) -> { 
      a.fault("boom");
      faulted.set(true);
    });
    
    system.tell(ActorRef.of(SINK));
    system.shutdownQuietly();
    assertTrue(faulted.get());
  }
  
  @Test
  public void testUnhandledFault() {
    final AtomicBoolean faulted = new AtomicBoolean();
    
    final ActorSystem system = system(1)
    .createActorSystem()
    .on(SINK).cue((a, m) -> { 
      a.fault("boom");
      faulted.set(true);
    })
    .ingress(a -> {
      a.to(ActorRef.of(SINK)).ask().onResponse(r -> {
        fail("Unexpected response");
      });
    });
    
    system.shutdownQuietly();
    assertTrue(faulted.get());
    assertEquals(1, system.getDeadLetterQueue().size());
  }
  
  @Test
  public void testDoubleFault() {
    final AtomicBoolean faulted = new AtomicBoolean();
    final AtomicBoolean handled = new AtomicBoolean();
    
    final ActorSystem system = system(1)
    .createActorSystem()
    .on(SINK).cue((a, m) -> { 
      a.fault("boom 1");
      faulted.set(true);
      throw new TestException("boom 2"); // should overwrite the prior fault
    })
    .ingress(a -> {
      a.to(ActorRef.of(SINK)).ask()
      .onFault(f -> {
        assertFalse(handled.get());
        assertEquals("boom 2", f.<Exception>getReason().getMessage());
        handled.set(true);
      })
      .onResponse(r -> {
        fail("Unexpected response");
      });
    });
    
    system.shutdownQuietly();
    assertTrue(faulted.get());
    assertTrue(handled.get());
    assertEquals(1, system.getDeadLetterQueue().size());
  }
  
  @Test
  public void testFaultAfterTimeout() {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final AtomicBoolean faulted = new AtomicBoolean();
    final AtomicBoolean timedOut = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(SINK).cue((a, m) -> {
      // delay the fault so that it gets beaten by the timeout
      TestSupport.await(barrier);
      a.fault("delayed boom");
      faulted.set(true);
    })
    .ingress(a -> {
      a.to(ActorRef.of(SINK)).ask()
      .await(1)
      .onTimeout(() -> {
        TestSupport.await(barrier);
        timedOut.set(true);
      })
      .onFault(f -> {
        fail("Unexpected fault");
      })
      .onResponse(r -> {
        fail("Unexpected response");
      });
    })
    .shutdownQuietly();
    
    assertTrue(faulted.get());
    assertTrue(timedOut.get());
  }
  
  private void syncOrAsync(Activation a, Executor external, boolean async, Runnable run) {
    if (async) {
      a.egress(() -> null)
      .withExecutor("ext")
      .await(60_000).onTimeout(() -> {
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
