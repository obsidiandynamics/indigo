package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public class StatelessChainTest {
  private static final String RUN = "run";
  private static final String DONE = "done";
  
  @Test
  public void test() {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> completed = new HashSet<>();

    new ActorSystem()
    .when(RUN).apply(a -> {
      final int msg = a.message().body();
      if (msg < runs) {
        a.toSelf().tell(msg + 1);
      } else {
        a.to(ActorRef.of(DONE)).tell(a.self());
      }
    })
    .when(DONE).apply(a -> completed.add(a.message().body()))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        a.to(ActorRef.of(RUN, i + "")).tell(1);
      }
    })
    .shutdown();

    assertEquals(actors, completed.size());
  }
}
