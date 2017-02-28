package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class RequestResponseTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String ADDER = "adder";
  private static final String DONE_RUNS = "done_runs";

  @Test
  public void test() {
    logTestName();
    
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();

    new ActorSystemConfig() {}
    .define()
    .when(DRIVER).lambda(IntegerState::new, (a, s) -> {
      a.to(ActorRef.of(ADDER)).ask(s.value).response(r -> {
        final int res = r.message().body();
        if (res == runs) {
          a.to(ActorRef.of(DONE_RUNS)).tell();
        } else {
          assertEquals(s.value + 1, res);
          s.value = res;
          r.toSelf().tell();
        }
      });
    })
    .when(ADDER).lambda(a -> {
      a.reply(a.message().<Integer>body() + 1);
    })
    .when(DONE_RUNS).lambda(refCollector(doneRuns))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        a.to(ActorRef.of(DRIVER, i + "")).tell();
      }
    })
    .shutdown();

    assertEquals(actors, doneRuns.size());
  }
}