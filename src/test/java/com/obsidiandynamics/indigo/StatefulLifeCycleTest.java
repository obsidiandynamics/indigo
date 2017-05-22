package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

public final class StatefulLifeCycleTest implements TestSupport {
  private static final int SCALE = 1;
  
  private static final String TARGET = "target";
  private static final String EXTERNAL = "external";
  
  private static class MockDB {
    private final Map<ActorRef, IntegerState> states = new ConcurrentHashMap<>();
    
    IntegerState get(ActorRef ref) {
      if (! states.containsKey(ref)) {
        final IntegerState state = new IntegerState();
        states.put(ref, state);
        return state;
      } else {
        return states.get(ref);
      }
    }
    
    void put(ActorRef ref, IntegerState state) {
      states.put(ref, state);
    }
  }

  @Test
  public void testSyncUnbiased() {
    test(false, 1_000 * SCALE, 1);
  }

  @Test
  public void testSyncBiased() {
    test(false, 1_000 * SCALE, 10);
  }

  @Test
  public void testAsyncUnbiased() {
    test(true, 1_000 * SCALE, 1);
  }

  @Test
  public void testAsyncBiased() {
    test(true, 1_000 * SCALE, 10);
  }

  private void test(boolean async, int n, int actorBias) {
    final AtomicBoolean activating = new AtomicBoolean();
    final AtomicBoolean activated = new AtomicBoolean();
    final AtomicBoolean passivating = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean(true);
    
    final AtomicInteger activationCount = new AtomicInteger();
    final AtomicInteger actCount = new AtomicInteger();
    final AtomicInteger passivationCount = new AtomicInteger();
    
    final MockDB db = new MockDB();
    final Executor external = r -> new Thread(r, EXTERNAL).start();
    
    new TestActorSystemConfig() {{
      parallelism = Runtime.getRuntime().availableProcessors();
      defaultActorConfig = new ActorConfig() {{
        bias = actorBias;
        backlogThrottleCapacity = 10;
      }};
    }}
    .createActorSystem()
    .addExecutor(external).named("ext")
    .on(TARGET)
    .cue(StatefulLambdaActor
         .<IntegerState>builder()
         .activated(a -> {
           log("activating\n");
           assertFalse(activating.get());
           assertFalse(activated.get());
           assertFalse(passivating.get());
           assertTrue(passivated.get());
           activating.set(true);
           passivated.set(false);
           
           final CompletableFuture<IntegerState> f = new CompletableFuture<>();
           final Consumer<IntegerState> activator = saved -> {
             log("activated %s\n", saved);
             assertTrue(activating.get());
             assertFalse(activated.get());
             assertFalse(passivating.get());
             assertFalse(passivated.get());
             activating.set(false);
             activated.set(true);
             passivated.set(false);
             activationCount.incrementAndGet();
             f.complete(saved);
           };
           
           if (async) {
             a.egress(() -> db.get(a.self()))
             .withExecutor("ext")
             .ask()
             .onResponse(r -> activator.accept(r.body()));
           } else {
             activator.accept(db.get(a.self()));
           }
           return f;
         })
         .act((a, m, s) -> {
           log("act\n");
           assertFalse(activating.get());
           assertTrue(activated.get());
           assertFalse(passivating.get());
           assertFalse(passivated.get());
           actCount.incrementAndGet();
           
           final int body = m.body();
           assertEquals(s.value + 1, body);
           s.value = body;

           a.passivate();
         })
         .passivated((a, s) -> {
           log("passivating %s\n", s);
           assertFalse(activating.get());
           assertTrue(activated.get());
           assertFalse(passivating.get());
           assertFalse(passivated.get());
           activated.set(false);
           passivating.set(true);
           
           final Runnable passivator = () -> {
             log("passivated\n");
             assertFalse(activating.get());
             assertFalse(activated.get());
             assertTrue(passivating.get());
             assertFalse(passivated.get());
             passivating.set(false);
             passivated.set(true);
             passivationCount.incrementAndGet();
           };
           
           if (async) {
             a.egress(() -> db.put(a.self(), s))
             .withExecutor("ext")
             .ask()
             .onResponse(r -> passivator.run());
           } else {
             passivator.run();
             db.put(a.self(), s);
           }
         }))
    .ingress().act(a -> {
      for (int i = 1; i <= n; i++) {
        a.to(ActorRef.of(TARGET)).tell(i);
      }
    })
    .shutdownQuietly();

    assertEquals(n, db.get(ActorRef.of(TARGET)).value);

    assertFalse(activating.get());
    assertFalse(activated.get());
    assertFalse(passivating.get());
    assertTrue(passivated.get());
    
    assertTrue("activationCount=" + activationCount + ", passivationCount=" + passivationCount,
               activationCount.get() == passivationCount.get());
    assertTrue("activationCount=" + activationCount, activationCount.get() >= 1);
    assertTrue("activationCount=" + activationCount + ", n=" + n, activationCount.get() <= n);
    assertEquals(n, actCount.get());
    assertTrue("passivationCount=" + passivationCount, passivationCount.get() >= 1);
    
    log("passivations: %d\n", passivationCount.get());
  }
}
