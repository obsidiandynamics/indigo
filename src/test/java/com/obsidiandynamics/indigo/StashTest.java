package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public final class StashTest implements TestSupport {
  private static final String SINK = "sink";
  
  @Test
  public void test() {
    logTestName();
    
    final List<Integer> sequence = new ArrayList<>();

    new ActorSystemConfig() {}
    .define()
    .when(SINK).lambda((a, m) -> {
      final int body = m.body();
      if (body == 0) {
        a.stash(c -> c.<Integer>body() % 4 != 0);
      } else if (body == 8) {
        a.unstash();
      }
      sequence.add(body);
    })
    .ingress().times(9).act((a, i) -> a.to(ActorRef.of(SINK)).tell(i))
    .shutdown();

    assertEquals(Arrays.asList(0, 4, 8, 1, 2, 3, 5, 6, 7), sequence);
  }
}
