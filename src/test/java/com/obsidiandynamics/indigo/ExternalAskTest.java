package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.assertEquals;

import java.util.concurrent.*;

import org.junit.*;

public final class ExternalAskTest implements TestSupport {
  private static final String ADDER = "adder";

  @Test
  public void testResponse() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final ActorSystem system = new ActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> a.reply(m, m.<Integer>body() + 1));
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    final int resp = f.get();
    assertEquals(42, resp);
  }

  @Test(expected=TimeoutException.class)
  public void testTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final ActorSystem system = new ActorSystemConfig() {}
    .define()
    .when(ADDER).lambda((a, m) -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    f.get(10, TimeUnit.MILLISECONDS);
  }
}