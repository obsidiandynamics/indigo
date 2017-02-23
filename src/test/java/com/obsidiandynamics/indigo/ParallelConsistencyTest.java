package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.atomic.*;

import org.junit.*;

public class ParallelConsistencyTest {
  private static final String TEST_TYPE = "test";
  
  @Test
  public void test() throws InterruptedException {
    test(1, 10);
    test(10, 100);
    test(100, 1_000);
    test(10, 10_000);
  }
  
  private static void test(int actors, int runs) throws InterruptedException {
    final AtomicInteger completed = new AtomicInteger();
    try (ActorSystem system = new ActorSystem()) {
      system
      .when(TEST_TYPE).use(() -> new Actor() {
        private int state;
        
        @Override
        public void act(Activation a) {
          final int msg = a.message().body();
          if (msg != state + 1) {
            throw new IllegalStateException("Actor " + a.id() + " with state " + state + " got message " + msg);
          }
          state = msg;
          
          if (state == runs) {
            completed.incrementAndGet();
          }
        }
      })
      .enter(a -> {
        for (int i = 0; i < actors; i++) {
          for (int j = 1; j <= runs; j++) {
            a.to(ActorId.of(TEST_TYPE, i)).tell(j);
          }
        }
      })
      .await();

      assertEquals(actors, completed.get());
    }
  }
}
