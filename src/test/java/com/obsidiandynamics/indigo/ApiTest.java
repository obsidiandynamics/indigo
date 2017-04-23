package com.obsidiandynamics.indigo;

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
}
