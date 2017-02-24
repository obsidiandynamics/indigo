package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public class ParallelConsistencyTest implements TestSupport {
  private static final String RUN = "run";
  private static final String DONE = "done";

  private static class IntegerState {
    int value;
  }

  @Test
  public void test() {
    test(1, 10);
    test(10, 100);
    test(100, 1_000);
    test(10, 10_000);
  }

  private void test(int actors, int runs) {
    final Set<ActorRef> doneRun = new HashSet<>();

    new ActorSystem()
    .when(RUN).apply(IntegerState::new, (a, s) -> {
      final int msg = a.message().body();
      if (msg != s.value + 1) {
        throw new IllegalStateException("Actor " + a.self() + " with state " + s.value + " got message " + msg);
      }
      s.value = msg;

      if (s.value == runs) {
        a.to(ActorRef.of(DONE)).tell();
      }
    })
    .when(DONE).apply(refCollector(doneRun))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        for (int j = 1; j <= runs; j++) {
          a.to(ActorRef.of(RUN, i + "")).tell(j);
        }
      }
    })
    .shutdown();

    assertEquals(actors, doneRun.size());
  }
}
