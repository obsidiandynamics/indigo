package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

public final class StatefulLambdaActorApiTest implements TestSupport {
  @Test
  public void testNoAct() throws InterruptedException {
    try {
      StatelessLambdaActor.builder().build();
      fail("Failed to catch IllegalStateException");
    } catch (IllegalStateException e) {
      assertEquals("No on-act lambda has been assigned", e.getMessage());
    }
  }
}
