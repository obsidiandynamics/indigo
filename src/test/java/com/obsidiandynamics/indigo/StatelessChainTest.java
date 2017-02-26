package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class StatelessChainTest implements TestSupport {
  private static final String RUN = "run";
  private static final String DONE = "done";
  
  @Test
  public void test() {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRun = new HashSet<>();

    new ActorSystemConfig() {}
    .define()
    .when(RUN).lambda(a -> {
      final int msg = a.message().body();
      if (msg < runs) {
        a.toSelf().tell(msg + 1);
      } else {
        a.to(ActorRef.of(DONE)).tell();
      }
    })
    .when(DONE).lambda(refCollector(doneRun))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        a.to(ActorRef.of(RUN, i + "")).tell(1);
      }
    })
    .shutdown();

    assertEquals(actors, doneRun.size());
  }
}
