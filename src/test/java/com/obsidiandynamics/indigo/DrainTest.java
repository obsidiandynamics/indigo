package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class DrainTest implements TestSupport {
  private static final String STAGE = "stage-";
  private static final String SINK = "sink";

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
  
  @Test(expected=InterruptedException.class)
  public void testInterruptOnDrain() throws InterruptedException {
    final ActorSystem system = new TestActorSystemConfig() {}.define();
    
    Thread.currentThread().interrupt();
    try {
      system.drain(0);
    } finally {
      system.shutdownQuietly();
    }
    fail("Interrupt not detected");
  }
  
  @Test(expected=InterruptedException.class)
  public void testInterruptOnShutdown() throws InterruptedException {
    final ActorSystem system = new TestActorSystemConfig() {}.define();
    
    Thread.currentThread().interrupt();
    try {
      system.shutdown();
    } finally {
      system.shutdown();
    }
    fail("Interrupt not detected");
  }
  
  @Test
  public void testInterruptOnShutdownQuietly() {
    final ActorSystem system = new TestActorSystemConfig() {}.define();
    
    Thread.currentThread().interrupt();
    system.shutdownQuietly();
    assertTrue(Thread.interrupted());
    system.shutdownQuietly();
  }
  
  @Test
  public void testRemainingOnDrain() throws InterruptedException {
    final int actors = 1;
    final CyclicBarrier exit = new CyclicBarrier(actors + 1);
    
    final ActorSystem system = new TestActorSystemConfig() {{
      parallelism = actors + 1;
    }}
    .define()
    .when(SINK).lambda((a, m) -> {
      Threads.await(exit);
    })
    .ingress().times(actors).act((a, i) -> {
      a.to(ActorRef.of(SINK, String.valueOf(i))).tell();
    });

    final long remaining = system.drain(1);
    assertTrue("remaining=" + remaining, remaining >= 1);
    
    Threads.await(exit);
    system.shutdownQuietly();
  }
}
