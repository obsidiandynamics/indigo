package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ParallelConsistencyTest implements IndigoTestSupport {
  private static final String DRIVER = "driver";
  private static final String SINK = "sink";
  private static final String DONE = "done";

  @Test
  public void testStraightUnbiased() {
    test(1, 10, 1, 1);
    test(10, 100, 1, 1);
    test(100, 1_000, 1, 1);
    test(10, 10_000, 1, 1);
  }
  
  @Test
  public void testStraightBiased() {
    test(1, 10, 1, 1_000);
    test(10, 100, 1, 1_000);
    test(100, 1_000, 1, 1_000);
    test(10, 10_000, 1, 1_000);
  }
  
  @Test
  public void testFanInBiased() {
    test(1, 10, 10, 1_000);
    test(10, 100, 100, 1_000);
    test(10, 10_000, 10, 1_000);
  }

  private void test(int actors, int runs, int fanIn, int sinkBias) {
    final Set<ActorRef> doneRuns = new HashSet<>();

    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(DRIVER).withConfig(new ActorConfig() {{
      bias = 1;
    }})
    .cue((a, m) -> {
      final ActorRef target = ActorRef.of(SINK, String.valueOf(Integer.valueOf(a.self().key()) % actors));
      for (int j = 1; j <= runs; j++) {
        a.to(target).tell(j);
      }
      a.to(ActorRef.of(DONE)).tell();
    })
    .on(SINK).withConfig(new ActorConfig() {{
      bias = sinkBias;
      backlogThrottleCapacity = Integer.MAX_VALUE;
      backlogThrottleMillis = 1;
      backlogThrottleTries = 1;
    }})
    .cue(() -> new int[fanIn], (a, m, s) -> {
      final int msg = m.body();
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
    .on(DONE).cue(refCollector(doneRuns))
    .ingress().times(actors * fanIn).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell())
    .shutdownSilently();

    assertEquals(actors * (fanIn + 1), doneRuns.size());
  }
}
