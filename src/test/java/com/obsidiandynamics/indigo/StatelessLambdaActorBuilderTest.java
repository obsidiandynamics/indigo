package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

public final class StatelessLambdaActorBuilderTest implements TestSupport {
  @Test
  public void testNoAct() {
    try {
      StatefulLambdaActor.builder().activated(a -> null).build();
      fail("Failed to catch IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("No on-act lambda has been assigned", e.getMessage());
    }
  }
  
  @Test
  public void testNoActivation() {
    try {
      StatefulLambdaActor.builder().act((a, m, s) -> {}).build();
      fail("Failed to catch IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("No on-activated lambda has been assigned", e.getMessage());
    }
  }
}
