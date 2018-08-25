package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ActivationApiTest implements TestSupport {
  private static final String SINK = "sink";
  
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {}.createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownSilently();
  }
  
  @Test
  public void testBoundedAskWithoutTimeout() throws InterruptedException {
    system.getConfig().exceptionHandler = DRAIN;
    system
    .on(SINK).cue((a, m) -> {})
    .ingress(a -> {
      try {
        a.to(ActorRef.of(SINK)).ask().onTimeout(() -> {}).onResponse(r -> {});
        fail("Failed to catch IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        assertEquals("Only one of the timeout time or handler has been set", e.getMessage());
      }
    })
    .drain(0);
  }
  
  @Test
  public void testBoundedAskWithoutTimeoutHandler() throws InterruptedException {
    system.getConfig().exceptionHandler = DRAIN;
    system
    .on(SINK).cue((a, m) -> {})
    .ingress(a -> {
      try {
        a.to(ActorRef.of(SINK)).ask().await(60_000).onResponse(r -> {});
        fail("Failed to catch IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        assertEquals("Only one of the timeout time or handler has been set", e.getMessage());
      }
    })
    .drain(0);
  }
}
