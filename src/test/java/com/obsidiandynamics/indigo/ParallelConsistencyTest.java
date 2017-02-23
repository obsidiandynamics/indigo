package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

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
    final CountDownLatch latch = new CountDownLatch(actors);
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
            latch.countDown();
          }
        }
      });
      
      for (int i = 0; i < actors; i++) {
        for (int j = 1; j <= runs; j++) {
          system.send(new Message(ActorId.of(TEST_TYPE, i), j));
        }
      }
      
      latch.await();
    }
  }
}
