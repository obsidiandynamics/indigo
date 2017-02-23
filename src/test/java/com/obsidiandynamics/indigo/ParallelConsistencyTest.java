package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ParallelConsistencyTest.Type.*;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public class ParallelConsistencyTest {
  static enum Type {
    RUN, DONE
  }
  
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
  
  private static void test(int actors, int runs) {
    final Set<ActorId> completed = new HashSet<>();
    
    try (ActorSystem system = new ActorSystem()) {
      system
      .when(RUN).apply(IntegerState::new, (a, s) -> {
        final int msg = a.message().body();
        if (msg != s.value + 1) {
          throw new IllegalStateException("Actor " + a.id() + " with state " + s.value + " got message " + msg);
        }
        s.value = msg;
        
        if (s.value == runs) {
          a.to(ActorId.of(DONE)).tell(a.id());
        }
      })
      .when(DONE).apply(a -> completed.add(a.message().body()))
      .ingress(a -> {
        for (int i = 0; i < actors; i++) {
          for (int j = 1; j <= runs; j++) {
            a.to(ActorId.of(RUN, i)).tell(j);
          }
        }
      });
    }
    
    assertEquals(actors, completed.size());
  }
}
