package com.obsidiandynamics.indigo.task;

import org.junit.*;

import nl.jqno.equalsverifier.*;

public final class TaskTest {
  @Test
  public void testEqualsHashCode() {
    EqualsVerifier.forClass(Task.class).verify();
  }
}
