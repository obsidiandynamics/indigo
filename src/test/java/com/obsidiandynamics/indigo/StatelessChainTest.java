package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class StatelessChainTest implements TestSupport {
  private static final String RUN = "run";
  private static final String DONE_RUNS = "done";
  
  @Test
  public void test() {
    logTestName();
    
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();

    new ActorSystemConfig() {}
    .define()
    .when(RUN).lambda((a, m) -> {
      final int msg = m.body();
      if (msg < runs) {
        a.toSelf().tell(msg + 1);
      } else {
        a.to(ActorRef.of(DONE_RUNS)).tell();
      }
    })
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(RUN, String.valueOf(i))).tell(1))
    .shutdown();

    assertEquals(actors, doneRuns.size());
  }
}
