package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

public final class StatefulLambdaActorBuilderTest implements TestSupport {
  @Test
  public void testNoAct() {
    try {
      StatelessLambdaActor.builder().build();
      fail("Failed to catch IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("No on-act lambda has been assigned", e.getMessage());
    }
  }
}
