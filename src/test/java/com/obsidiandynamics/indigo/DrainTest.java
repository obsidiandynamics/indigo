package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.atomic.*;

import org.junit.*;

public final class DrainTest implements TestSupport {
  private static final String STAGE = "stage-";

  @Test
  public void testOneStage() throws InterruptedException {
    test(1, 1, 1);
    test(10, 10, 1);
  }
  
  @Test
  public void testTenStages() throws InterruptedException {
    test(1, 1, 10);
    test(10, 10, 10);
  }
  
  @Test
  public void testHundredStages() throws InterruptedException {
    test(1, 1, 100);
    test(10, 10, 100);
  }

  private void test(int actors, int runs, int stages) throws InterruptedException {
    logTestName();
    
    final AtomicInteger received = new AtomicInteger();
    final ActorSystem system = new TestActorSystemConfig() {}.define();
    
    for (int stage = 0; stage < stages; stage++) {
      final int _stage = stage;
      system.when(STAGE + stage).lambda((a, m) -> {
        if (_stage != stages - 1) {
          a.to(ActorRef.of(STAGE + (_stage + 1), a.self().key())).tell();
        } else {
          received.incrementAndGet();
        }
      });
    }
    
    for (int r = 0; r < runs; r++) {
      received.set(0);
      system.ingress().times(actors).act((a, i) -> a.to(ActorRef.of(STAGE + 0, String.valueOf(i))).tell());
      final long left = system.drain(0);
      assertEquals(0, left);
      assertEquals(actors, received.get());
    }
    
    system.shutdown();
  }
}
