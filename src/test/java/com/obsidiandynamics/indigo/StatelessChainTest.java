package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.StatelessChainTest.ActorType.*;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

public class StatelessChainTest {
  static enum ActorType {
    RUN, DONE
  }
  
  @Test
  public void test() {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorId> completed = new HashSet<>();
    
    try (ActorSystem system = new ActorSystem()) {
      system
      .when(RUN).apply(a -> {
        final int msg = a.message().body();
        if (msg < runs) {
          a.self().tell(msg + 1);
        } else {
          a.to(ActorId.of(DONE)).tell(a.id());
        }
      })
      .when(DONE).apply(a -> completed.add(a.message().body()))
      .ingress(a -> {
        for (int i = 0; i < actors; i++) {
          a.to(ActorId.of(RUN, i)).tell(1);
        }
      });
    }
    
    assertEquals(actors, completed.size());
  }
}
