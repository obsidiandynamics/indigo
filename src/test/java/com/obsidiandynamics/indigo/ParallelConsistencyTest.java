package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class ParallelConsistencyTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String SINK = "sink";
  private static final String DONE = "done";

//  @Test
//  public void testStraight() {
//    test(1, 10, 1);
//    test(10, 100, 1);
//    test(100, 1_000, 1);
//    test(10, 10_000, 1);
//  }
  
  @Test
  public void testFanIn() {
//    test(1, 10, 10);
//    test(10, 100, 100);
    test(100, 1000, 100);
//    test(10, 10_000, 10);
  }

  private void test(int actors, int runs, int fanIn) {
    logTestName();
    
    final Set<ActorRef> doneRuns = new HashSet<>();

    new TestActorSystemConfig() {{ 
      defaultActorConfig = new ActorConfig() {{
        bias = 1;
        backlogThrottleCapacity = 10000;
        backlogThrottleMillis = 1;
        backlogThrottleTries = 1;
      }};
    }}
    .define()
    .when(DRIVER).lambda((a, m) -> {
      final ActorRef target = ActorRef.of(SINK, String.valueOf(Integer.valueOf(a.self().key()) % actors));
      for (int j = 1; j <= runs; j++) {
        a.to(target).tell(j);
      }
      a.to(ActorRef.of(DONE)).tell();
    })
    .when(SINK).lambdaSync(() -> new int[fanIn], (a, m, s) -> {
      final int msg = m.body();
      if (msg % 100 == 0) System.out.println("threads=" + Thread.activeCount());
      log("got %d from %s\n", msg, m.from());
      final int actor = Integer.valueOf(m.from().key()) / actors;
      assertEquals(s[actor] + 1, msg);
      s[actor] = msg;

      if (msg == runs) {
        int sum = 0;
        for (int i = 0; i < fanIn; i++) {
          sum += s[i];
        }
        if (sum == runs * fanIn) {
          a.to(ActorRef.of(DONE)).tell();
        }
      }
    })
    .when(DONE).lambda(refCollector(doneRuns))
    .ingress().times(actors * fanIn).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdown();

    assertEquals(actors * (fanIn + 1), doneRuns.size());
  }
}
