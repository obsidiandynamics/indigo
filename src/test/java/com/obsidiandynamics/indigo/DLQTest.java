package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.util.IndigoTestSupport.*;
import static com.obsidiandynamics.indigo.FaultType.*;
import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class DLQTest implements IndigoTestSupport {
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
    
    system.shutdownSilently();

    assertEquals(limit, system.getDeadLetterQueue().size());
    assertEquals(limit, countFaults(ON_ACT, system.getDeadLetterQueue()));
  }
}
