package com.obsidiandynamics.indigo.linear;

import org.junit.*;

import com.obsidiandynamics.verifier.*;

public final class ExecutorOptionsTest {
  @Test
  public void testPojo() {
    PojoVerifier.forClass(ExecutorOptions.class).verify();
  }
  
  @Test
  public void testFluent() {
    FluentVerifier.forClass(ExecutorOptions.class).verify();
  }
}
