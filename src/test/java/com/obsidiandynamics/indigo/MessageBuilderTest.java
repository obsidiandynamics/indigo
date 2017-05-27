package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class MessageBuilderTest implements TestSupport {
  @Test
  public void testNoAct() {
    try {
      Message.builder().build();
      fail("Failed to catch IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Destination not specified", e.getMessage());
    }
  }
}
