package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class ParallelConsistencyTest implements TestSupport {
  private static final String RUN = "run";
  private static final String DONE_RUNS = "done";

  @Test
  public void test() {
    test(1, 10);
    test(10, 100);
    test(100, 1_000);
    test(10, 10_000);
  }

  private void test(int actors, int runs) {
    final Set<ActorRef> doneRuns = new HashSet<>();

    new ActorSystemConfig() {}
    .define()
    .when(RUN).lambda(IntegerState::new, (a, s) -> {
      final int msg = a.message().body();
      assertEquals(s.value + 1, msg);
      s.value = msg;

      if (s.value == runs) {
        a.to(ActorRef.of(DONE_RUNS)).tell();
      }
    })
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        for (int j = 1; j <= runs; j++) {
          a.to(ActorRef.of(RUN, i + "")).tell(j);
        }
      }
    })
    .shutdown();

    assertEquals(actors, doneRuns.size());
  }
}
