package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ForwarderTest implements TestSupport {
  private static final String FORWARDER = "forwarder";
  private static final String SINK = "sink";
  
  @Test
  public void test() {
    test(10);
  }

  private void test(int runs) {
    final AtomicInteger forwarded = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    
    final ActorSystem system = ActorSystem.create()
    .on(FORWARDER).cue((a, m) -> {
      a.forward(m).to(ActorRef.of(SINK));
      forwarded.incrementAndGet();
    })
    .on(SINK).cue((a, m) -> {
      assertEquals(ActorRef.INGRESS, m.from().role());
      received.incrementAndGet();
    })
    .ingress().times(runs).act((a, i) -> a.to(ActorRef.of(FORWARDER)).tell());
    
    system.shutdownQuietly();

    assertEquals(runs, forwarded.get());
    assertEquals(runs, received.get());
  }
}
