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
    logTestName();
    
    final ActorSystem system = new TestActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> a.reply(m).tell(m.<Integer>body() + 1));
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    try {
      final int resp = f.get();
      assertEquals(42, resp);
    } finally {
      system.shutdownQuietly();
    }
  }

  @Test
  public void testCancel() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final AtomicBoolean ran = new AtomicBoolean();
    final CyclicBarrier barrier = new CyclicBarrier(2);
    
    final ActorSystem system = new TestActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> {
      a.reply(m).tell(m.<Integer>body() + 1);
      ran.set(true);
    });
    
    // block the ingress lambda using a barrier, preventing further message delivery
    system.ingress(a -> {
      Threads.await(barrier);
    });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    f.cancel(false);
    
    f.cancel(false); // cancelling a second time should have no further effect
    
    // unblock the ingress lambda; it should not deliver the cancelled message to the adder
    Threads.await(barrier);
    
    system.drain(0);
    
    assertFalse(ran.get());
    system.shutdownQuietly();
  }

  @Test(expected=TimeoutException.class)
  public void testTimeoutWithCancel() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final ActorSystem system = new TestActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER));
    try {
      f.get(10, TimeUnit.MILLISECONDS);
    } finally {
      f.cancel(false);
      f.cancel(false); // cancelling a second time should have no further effect
      system.shutdownQuietly();
    }
  }

  @Test(expected=TimeoutException.class)
  public void testTimeoutWithForce() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final ActorSystem system = new TestActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    try {
      f.get(10, TimeUnit.MILLISECONDS);
    } finally {
      system.forceTimeout();
      system.shutdownQuietly();
    }
  }
}