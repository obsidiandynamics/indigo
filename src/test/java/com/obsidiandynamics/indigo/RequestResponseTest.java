package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class RequestResponseTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String ADDER = "adder";
  private static final String DONE_RUNS = "done_runs";
  private static final String SINK = "SINK";

  @Test
  public void testRequestResponse() {
    logTestName();
    
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();

    new TestActorSystemConfig() {}
    .define()
    .when(DRIVER).lambdaSync(IntegerState::new, (a, m, s) -> {
      a.to(ActorRef.of(ADDER)).ask(s.value).onResponse(r -> {
        final int res = r.body();
        if (res == runs) {
          a.to(ActorRef.of(DONE_RUNS)).tell();
        } else {
          assertEquals(s.value + 1, res);
          s.value = res;
          a.toSelf().tell();
        }
      });
    })
    .when(ADDER).lambda((a, m) -> a.reply(m).tell(m.<Integer>body() + 1))
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdownQuietly();

    assertEquals(actors, doneRuns.size());
  }
  
  @Test
  public void testUnsolicitedReply() {
    logTestName();
    
    final Set<String> receivedRoles = new HashSet<>();
    new TestActorSystemConfig() {}
    .define()
    .when(DRIVER).lambda((a, m) -> {
      receivedRoles.add(m.from().role());
      if (m.from().isIngress()) {
        a.to(ActorRef.of(SINK)).tell();
      }
    })
    .when(SINK).lambda((a, m) -> {
      a.to(ActorRef.of(DRIVER)).tell();
    })
    .ingress(a -> a.to(ActorRef.of(DRIVER)).tell())
    .shutdownQuietly();
    
    assertEquals(2, receivedRoles.size());
    assertTrue(receivedRoles.contains(ActorRef.INGRESS));
    assertTrue(receivedRoles.contains(SINK));
  }
}