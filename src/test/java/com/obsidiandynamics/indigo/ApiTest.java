package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;

import org.junit.*;

public final class ApiTest implements TestSupport {
  private static final String SINK = "sink";
  
  @Test(expected=IllegalStateException.class)
  public void testDuplicateRoleRegistration() {
    final ActorSystem system = new TestActorSystemConfig() {}.define();
    
    system.when(SINK).lambda((a, m) -> {});
    try {
      system.when(SINK).lambda((a, m) -> {});
    } finally {
      system.shutdownQuietly();
    }
  }
  
  @Test(expected=UnhandledMultiException.class)
  public void testWithoutRole() throws InterruptedException {
    final ActorSystem system = new TestActorSystemConfig() {{
      exceptionHandler = DRAIN;
    }}
    .define()
    .ingress(a -> a.to(ActorRef.of(SINK)).tell());
    
    try {
      system.drain(0);
    } finally {
      system.shutdownQuietly();
    }
  }
}
