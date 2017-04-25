package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

public final class StatelessLambdaActorApiTest implements TestSupport {
  @Test
  public void testNoAct() throws InterruptedException {
    try {
      StatefulLambdaActor.builder().activated(a -> null).build();
      fail("Failed to catch IllegalStateException");
    } catch (IllegalStateException e) {
      assertEquals("No on-act lambda has been assigned", e.getMessage());
    }
  }
  
  @Test
  public void testNoActivation() throws InterruptedException {
    try {
      StatefulLambdaActor.builder().act((a, m, s) -> {}).build();
      fail("Failed to catch IllegalStateException");
    } catch (IllegalStateException e) {
      assertEquals("No on-activated lambda has been assigned", e.getMessage());
    }
  }
}
