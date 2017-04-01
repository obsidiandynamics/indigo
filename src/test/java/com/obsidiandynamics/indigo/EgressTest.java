package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;
import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

public final class EgressTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String DONE_RUNS = "done_runs";
  private static final String EXTERNAL = "external";

  @Test
  public void test() {
    logTestName();
    
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();
    
    final Executor external = r -> new Thread(r, EXTERNAL).start();

    new TestActorSystemConfig() {{
      parallelism = 1;
      executor = FIXED_THREAD_POOL;
    }}
    .define()
    .when(DRIVER).lambdaSync(IntegerState::new, (a, m, s) -> {
      a.<Integer, Integer>egress(in -> {
        assertEquals(EXTERNAL, Thread.currentThread().getName());
        return in + 1; 
      })
      .using(external)
      .ask(s.value).onResponse(r -> {
        assertFalse("Driven by an external thread", Thread.currentThread().getName().equals(EXTERNAL));
        
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