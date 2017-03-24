package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class ParallelConsistencyTest implements TestSupport {
  private static final String DRIVER = "driver";
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
    logTestName();
    
    final Set<ActorRef> doneRuns = new HashSet<>();

    new ActorSystemConfig() {{ 
      defaultActorConfig = new ActorConfig() {{
        bias = 100;
      }};
    }}
    .define()
    .when(DRIVER).lambda((a, m) -> {
      final ActorRef target = m.body();
      for (int j = 1; j <= runs; j++) {
        a.to(target).tell(j);
      }
      a.to(ActorRef.of(DONE_RUNS)).tell();
    })
    .when(RUN).lambda(IntegerState::new, (a, m, s) -> {
      final int msg = m.body();
      assertEquals(s.value + 1, msg);
      s.value = msg;

      if (s.value == runs) {
        a.to(ActorRef.of(DONE_RUNS)).tell();
      }
    })
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(ActorRef.of(RUN, String.valueOf(i))))
    .dispose();

    assertEquals(actors * 2, doneRuns.size());
  }
}
