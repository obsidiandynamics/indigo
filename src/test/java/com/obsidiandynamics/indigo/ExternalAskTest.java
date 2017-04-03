package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.assertEquals;

import java.util.concurrent.*;

import org.junit.*;

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
      system.shutdown();
    }
  }

  @Test(expected=TimeoutException.class)
  public void testTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final ActorSystem system = new TestActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 1_000, 41);
    try {
      f.get(10, TimeUnit.MILLISECONDS);
    } finally {
      f.cancel(false);
      system.shutdown();
    }
  }
}