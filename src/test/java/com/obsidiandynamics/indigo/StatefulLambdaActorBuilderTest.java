package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

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
