package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ParallelConsistencyTest.Type.*;
import static junit.framework.TestCase.*;
import java.util.*;

import org.junit.*;

public class ParallelConsistencyTest {
  static enum Type {
    RUN, DONE
  }
  
  @Test
  public void test() throws InterruptedException {
    test(1, 10);
    test(10, 100);
    test(100, 1_000);
    test(10, 10_000);
  }
  
  private static void test(int actors, int runs) {
    final Set<Activation> completed = new HashSet<>();
    
    try (ActorSystem system = new ActorSystem()) {
      system
      .when(RUN).use(() -> new Actor() {
        private int state;
        
        @Override
        public void act(Activation a) {
          final int msg = a.message().body();
          if (msg != state + 1) {
            throw new IllegalStateException("Actor " + a.id() + " with state " + state + " got message " + msg);
          }
          state = msg;
          
          if (state == runs) {
            a.to(ActorId.of(DONE, 0)).tell(a.id());
          }
        }
      })
      .when(DONE).apply(completed::add)
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
