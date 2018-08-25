package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ExternalAskTest implements TestSupport {
  private static final String ADDER = "adder";

  @Test
  public void testResponse() throws InterruptedException, ExecutionException, TimeoutException {
    final ActorSystem system = new TestActorSystemConfig() {}
    .createActorSystem()
    .on(ADDER).cue((a, m) -> a.reply(m).tell(m.<Integer>body() + 1));
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    try {
      final int resp = f.get();
      assertEquals(42, resp);
    } finally {
      system.shutdownSilently();
    }
  }

  @Test
  public void testCancel() throws InterruptedException, ExecutionException, TimeoutException {
    final AtomicBoolean ran = new AtomicBoolean();
    final CyclicBarrier barrier = new CyclicBarrier(2);
    
    final ActorSystem system = new TestActorSystemConfig() {{
      ingressCount = 1; // locking this down to 1 so that we can artificially block the ingress within the test
    }}
    .createActorSystem()
    .on(ADDER).cue((a, m) -> {
      a.reply(m).tell(m.<Integer>body() + 1);
      ran.set(true);
    });
    
    // block the ingress lambda using a barrier, preventing further message delivery
    system.ingress(a -> {
      TestSupport.await(barrier);
    });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    f.cancel(false);
    
    f.cancel(false); // cancelling a second time should have no further effect
    
    // unblock the ingress lambda; it should not deliver the cancelled message to the adder
    TestSupport.await(barrier);
    
    system.drain(0);
    
    assertFalse(ran.get());
    system.shutdownSilently();
  }

  @Test(expected=TimeoutException.class)
  public void testTimeoutWithCancel() throws InterruptedException, ExecutionException, TimeoutException {
    final ActorSystem system = new TestActorSystemConfig() {}
    .createActorSystem()
    .on(ADDER).cue((a, m) -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER));
    try {
      f.get(10, TimeUnit.MILLISECONDS);
    } finally {
      f.cancel(false);
      f.cancel(false); // cancelling a second time should have no further effect
      system.shutdownSilently();
    }
  }

  @Test(expected=TimeoutException.class)
  public void testTimeoutWithForce() throws InterruptedException, ExecutionException, TimeoutException {
    final ActorSystem system = new TestActorSystemConfig() {}
    .createActorSystem()
    .on(ADDER).cue((a, m) -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    try {
      f.get(10, TimeUnit.MILLISECONDS);
    } finally {
      system.forceTimeout();
      system.shutdownSilently();
    }
  }
  
  @Test
  public void testFaultString() throws InterruptedException, TimeoutException {
    final ActorSystem system = new TestActorSystemConfig() {}
    .createActorSystem()
    .on(ADDER).cue((a, m) -> a.fault("some reason"));
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    try {
      f.get();
      fail("Failed to catch ExecutionException");
    } catch (ExecutionException e) {
      assertEquals(FaultException.class, e.getCause().getClass());
      assertEquals("some reason", ((FaultException) e.getCause()).getReason());
    } finally {
      system.shutdownSilently();
    }
  }
  
  @Test
  public void testFaultException() throws InterruptedException, TimeoutException {
    final ActorSystem system = new TestActorSystemConfig() {{
      exceptionHandler = TestException.BYPASS_DRAIN_HANDLER;
    }}
    .createActorSystem()
    .on(ADDER).cue((a, m) -> {
      TestSupport.sleep(5);
      throw new TestException("some reason");
    });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    try {
      f.get();
      fail("Failed to catch ExecutionException");
    } catch (ExecutionException e) {
      assertEquals(TestException.class, e.getCause().getClass());
      assertEquals("some reason", e.getCause().getMessage());
    } finally {
      system.shutdownSilently();
    }
  }
}