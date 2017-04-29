package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.FaultType.*;
import static com.obsidiandynamics.indigo.TestSupport.*;
import static junit.framework.TestCase.*;

import org.junit.*;

public final class DLQTest implements TestSupport {
  private static final String SINK = "sink";
  
  @Test
  public void testLimitOnAct() {
    test(10);
  }

  private void test(int limit) {
    final ActorSystem system = new TestActorSystemConfig() {{
      deadLetterQueueSize = limit;
    }}
    .createActorSystem()
    .on(SINK).cue((a, m) -> {
      a.fault("something happened");
    })
    .ingress().times(limit * 2).act((a, i) -> a.to(ActorRef.of(SINK)).tell());
    
    system.shutdownQuietly();

    assertEquals(limit, system.getDeadLetterQueue().size());
    assertEquals(limit, countFaults(ON_ACT, system.getDeadLetterQueue()));
  }
}
