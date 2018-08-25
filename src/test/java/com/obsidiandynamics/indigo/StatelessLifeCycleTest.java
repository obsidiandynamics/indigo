package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

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
    .createActorSystem()
    .on(TARGET)
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
           assertFalse(activating.get());
           assertFalse(activated.get());
           assertFalse(passivating.get());
           assertTrue(passivated.get());
           activating.set(true);
           passivated.set(false);
           
           syncOrAsync(a, async, () -> {
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

           syncOrAsync(a, async, () -> {
             log("passivated\n");
             assertFalse(activating.get());
             assertFalse(activated.get());
             assertTrue(passivating.get());
             assertFalse(passivated.get());
             passivating.set(false);
             passivated.set(true);
             passivationCount.incrementAndGet();
           });
         }))
    .on(ECHO).cue((a, m) -> a.reply(m).tell())
    .ingress().act(a -> {
      for (int i = 0; i < n; i++) {
        a.to(ActorRef.of(TARGET)).tell(i);
      }
    })
    .shutdownSilently();

    assertEquals(sequenceTo(n), received);
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
  
  private void syncOrAsync(Activation a, boolean async, Runnable run) {
    if (async) {
      // ask once and wait for a response
      a.to(ActorRef.of(ECHO)).ask().onResponse(r -> {
        // ask a second time... for good measure
        a.to(ActorRef.of(ECHO)).ask().onResponse(r2 -> {
          run.run();
        });
      });
    } else {
      run.run();
    }
  }
  
  private static List<Integer> sequenceTo(int n) {
    final List<Integer> sequence = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      sequence.add(i);
    }
    return sequence;
  }
  
  @Test
  public void testPassivationRequestOnAsyncActivation() {
    final AtomicBoolean activated = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(TARGET)
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
           a.egress(() -> null)
           .ask()
           .onResponse(r -> {
             activated.set(true);
           });

           a.passivate();
         })
         .act((a, m) -> {
         })
         .passivated(a -> {
           log("passivating\n");
           passivated.set(true);
         }))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell())
    .shutdownSilently();
    
    assertTrue(activated.get());
    assertTrue(passivated.get());
  }
  
  @Test
  public void testPassivationRequestOnAsyncAct() {
    final AtomicBoolean responded = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(TARGET)
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
         })
         .act((a, m) -> {
           log("act\n");
           a.egress(() -> null)
           .ask()
           .onResponse(r -> {
             responded.set(true);
           });

           a.passivate();
         })
         .passivated(a-> {
           log("passivating\n");
           passivated.set(true);
         }))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell())
    .shutdownSilently();
    
    assertTrue(responded.get());
    assertTrue(passivated.get());
  }
  
  @Test
  public void testPassivationRequestWhileStashing() {
    final AtomicBoolean acted = new AtomicBoolean();
    final AtomicBoolean passivated = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(TARGET)
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
         })
         .act((a, m) -> {
           log("act\n");
           acted.set(true);
           a.stash(Functions::alwaysTrue);

           a.passivate();
         })
         .passivated(a -> {
           log("passivating\n");
           passivated.set(true);
         }))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell())
    .shutdownSilently();
    
    assertTrue(acted.get());
    assertFalse(passivated.get());
  }

  @Test
  public void testUnpassivate() {
    final AtomicInteger activated = new AtomicInteger();
    final AtomicInteger acted = new AtomicInteger();
    final AtomicBoolean passivated = new AtomicBoolean();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(TARGET)
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
           activated.incrementAndGet();
         })
         .act((a, m) -> {
           log("act\n");
           acted.incrementAndGet();

           a.passivate();
           a.unpassivate();
         })
         .passivated(a -> {
           log("passivating\n");
           passivated.set(true);
         }))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell())
    .shutdownSilently();
    
    assertEquals(1, activated.get());
    assertEquals(1, acted.get());
    assertFalse(passivated.get());
  }
  
  @Test
  public void testSleepingPill() {
    final AtomicInteger activated = new AtomicInteger();
    final AtomicInteger acted = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(TARGET)
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
           activated.incrementAndGet();
         })
         .act((a, m) -> {
           log("act\n");
           acted.incrementAndGet();
         })
         .passivated(a -> {
           log("passivating\n");
           passivated.incrementAndGet();
         }))
    .ingress().act(a -> {
      a.to(ActorRef.of(TARGET)).tell();
      a.to(ActorRef.of(TARGET)).tell(SleepingPill.instance());
    })
    .shutdownSilently();
    
    assertEquals(1, activated.get());
    assertEquals(1, acted.get());
    assertEquals(1, passivated.get());
  }
  
  @Test
  public void testEphemeral() {
    final AtomicInteger activated = new AtomicInteger();
    final AtomicInteger acted = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
    
    new TestActorSystemConfig() {}
    .createActorSystem()
    .on(TARGET).withConfig(new ActorConfig() {{ ephemeral = true; }})
    .cue(StatelessLambdaActor
         .builder()
         .activated(a -> {
           log("activating\n");
           activated.incrementAndGet();
         })
         .act((a, m) -> {
           log("act\n");
           acted.incrementAndGet();
         })
         .passivated(a -> {
           log("passivating\n");
           passivated.incrementAndGet();
         }))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell())
    .shutdownSilently();
    
    assertEquals(1, activated.get());
    assertEquals(1, acted.get());
    assertEquals(1, passivated.get());
  }
}
