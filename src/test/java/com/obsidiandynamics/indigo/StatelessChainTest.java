package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class StatelessChainTest implements TestSupport {
  private static final String RUN = "run";
  private static final String DONE_RUNS = "done";
  
  @Test
  public void test() {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();

    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(RUN).cue((a, m) -> {
      final int body = m.body();
      if (body < runs) {
        a.toSelf().tell(body + 1);
      } else {
        a.to(ActorRef.of(DONE_RUNS)).tell();
      }
    })
    .on(DONE_RUNS).cue(refCollector(doneRuns))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(RUN, String.valueOf(i))).tell(1))
    .shutdownQuietly();

    assertEquals(actors, doneRuns.size());
  }
}
