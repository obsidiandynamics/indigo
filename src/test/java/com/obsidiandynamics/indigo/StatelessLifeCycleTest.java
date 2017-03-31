package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

public final class StatelessLifeCycleTest implements TestSupport {
  private static final int SCALE = 1;
  
  private static final String TARGET = "target";
  private static final String ECHO = "echo";

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
    logTestName();
    
    final List<Integer> received = new ArrayList<>();
    
    final AtomicBoolean activating = new AtomicBoolean();
    final AtomicBoolean activated = new AtomicBoolean();
    final AtomicBoolean passivating = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean(true);
    
    final AtomicInteger activationCount = new AtomicInteger();
    final AtomicInteger actCount = new AtomicInteger();
    final AtomicInteger passivationCount = new AtomicInteger();
    
    new TestActorSystemConfig() {{
      parallelism = Runtime.getRuntime().availableProcessors();
      defaultActorConfig = new ActorConfig() {{
        bias = actorBias;
        backlogThrottleCapacity = 10;
      }};
    }}
    .define()
    .when(TARGET)
    .use(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
           assertFalse(activating.get());
           assertFalse(activated.get());
           assertFalse(passivating.get());
           assertTrue(passivated.get());
           activating.set(true);
           passivated.set(false);
           
           if (async) {
             a.to(ActorRef.of(ECHO)).ask().onResponse(r -> {
               // ask a second time... for good measure
               a.to(ActorRef.of(ECHO)).ask().onResponse(r2 -> {
                 log("activated\n");
                 assertTrue(activating.get());
                 assertFalse(activated.get());
                 assertFalse(passivating.get());
                 assertFalse(passivated.get());
                 activating.set(false);
                 activated.set(true);
                 passivated.set(false);
                 activationCount.incrementAndGet();
               });
             });
           } else {
             log("activated\n");
             assertTrue(activating.get());
             assertFalse(activated.get());
             assertFalse(passivating.get());
             assertFalse(passivated.get());
             activating.set(false);
             activated.set(true);
             passivated.set(false);
             activationCount.incrementAndGet();
           }
         })
         .act((a, m) -> {
           log("act %d\n", m.<Integer>body());
           assertFalse(activating.get());
           assertTrue(activated.get());
           assertFalse(passivating.get());
           assertFalse(passivated.get());
           a.passivate();
           received.add(m.body());
           actCount.incrementAndGet();
         })
         .passivated(a -> {
           log("passivating\n");
           assertFalse(activating.get());
           assertTrue(activated.get());
           assertFalse(passivating.get());
           assertFalse(passivated.get());
           activated.set(false);
           passivating.set(true);

           if (async) {
             // ask once and wait for a response
             a.to(ActorRef.of(ECHO)).ask().onResponse(r -> {
               // ask a second time... for good measure
               a.to(ActorRef.of(ECHO)).ask().onResponse(r2 -> {
                 log("passivated\n");
                 assertFalse(activating.get());
                 assertFalse(activated.get());
                 assertTrue(passivating.get());
                 assertFalse(passivated.get());
                 passivating.set(false);
                 passivated.set(true);
                 passivationCount.incrementAndGet();
               });
             });
           } else {
             log("passivated\n");
             assertFalse(activating.get());
             assertFalse(activated.get());
             assertTrue(passivating.get());
             assertFalse(passivated.get());
             passivating.set(false);
             passivated.set(true);
             passivationCount.incrementAndGet();
           }
         }))
    .when(ECHO).lambda((a, m) -> a.reply(m).tell())
    .ingress().act(a -> {
      for (int i = 0; i < n; i++) {
        a.to(ActorRef.of(TARGET)).tell(i);
      }
    })
    .shutdown();

    assertEquals(sequenceTo(n), received);
    assertFalse(activating.get());
    assertFalse(activated.get());
    assertFalse(passivating.get());
    assertTrue(passivated.get());
    
    assertTrue(activationCount.get() == passivationCount.get());
    assertTrue(activationCount.get() >= 1);
    assertTrue(activationCount.get() <= n);
    assertEquals(n, actCount.get());
    assertTrue(passivationCount.get() >= 1);
    
    log("passivations: %d\n", passivationCount.get());
  }
  
  private static List<Integer> sequenceTo(int n) {
    final List<Integer> sequence = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      sequence.add(i);
    }
    return sequence;
  }
}
