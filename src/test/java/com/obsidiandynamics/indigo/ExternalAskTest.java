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
    .when(ADDER).lambda(a -> a.reply(a.message().<Integer>body() + 1));
    
    system.ask(ActorRef.of(ADDER), 41).whenComplete((resp, throwable) -> assertEquals(42, (int) resp));
  }

  @Test(expected=TimeoutException.class)
  public void testTimeout() throws InterruptedException, ExecutionException, TimeoutException {
    logTestName();
    
    final ActorSystem system = new ActorSystemConfig() {}
    .define()
    .when(ADDER).lambda(a -> { /* do nothing, stalling the reply */ });
    
    final CompletableFuture<Integer> f = system.ask(ActorRef.of(ADDER), 41);
    f.get(10, TimeUnit.MILLISECONDS);
  }
}