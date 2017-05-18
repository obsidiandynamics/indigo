package com.obsidiandynamics.indigo;

import static java.util.concurrent.TimeUnit.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

public final class ReaperTest implements TestSupport {
  private static final String TARGET = "target";
  
  private static final class Counters {
    final AtomicInteger activated = new AtomicInteger();
    final AtomicInteger acted = new AtomicInteger();
    final AtomicInteger passivated = new AtomicInteger();
  }
  
  private ActorSystem system;
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test
  public void testShortTimeoutReap() throws InterruptedException {
    final Counters counters = new Counters();
    
    system = new TestActorSystemConfig() {{
      reaperPeriodMillis = 1;
    }}
    .createActorSystem()
    .on(TARGET).withConfig(new ActorConfig() {{
      reapTimeoutMillis = 1;
    }})
    .cue(instrumentedActor(counters))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell());
    
    system.drain(0);

    assertTrue("activated=" + counters.activated, counters.activated.get() >= 1);
    assertEquals(1, counters.acted.get());

    await().atMost(60, SECONDS).until(() -> counters.passivated.get() >= 1);
    assertTrue("passivated=" + counters.passivated, counters.passivated.get() >= 1);
  }
  
  @Test
  public void testShortTimeoutForcedReap() throws InterruptedException {
    final Counters counters = new Counters();
    
    system = new TestActorSystemConfig() {{
      reaperPeriodMillis = 600_000;
    }}
    .createActorSystem()
    .on(TARGET).withConfig(new ActorConfig() {{
      reapTimeoutMillis = 1;
    }})
    .cue(instrumentedActor(counters))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell());
    
    system.drain(0);

    assertEquals(1, counters.activated.get());
    assertEquals(1, counters.acted.get());
    assertEquals(0, counters.passivated.get());
    
    await().atMost(60, SECONDS).until(() -> {
      system.reap();
      return counters.passivated.get() >= 1;
    });
    assertTrue("passivated=" + counters.passivated, counters.passivated.get() >= 1);
  }
  
  @Test
  public void testShortTimeoutNoReap() throws InterruptedException {
    final Counters counters = new Counters();
    
    system = new TestActorSystemConfig() {{
      reaperPeriodMillis = 0;
    }}
    .createActorSystem()
    .on(TARGET).withConfig(new ActorConfig() {{
      reapTimeoutMillis = 1;
    }})
    .cue(instrumentedActor(counters))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell());
    
    system.drain(0);

    assertEquals(1, counters.activated.get());
    assertEquals(1, counters.acted.get());

    TestSupport.sleep(200);
    assertEquals(0, counters.passivated.get());
  }
  
  @Test
  public void testLongTimeoutReap() throws InterruptedException {
    final Counters counters = new Counters();
    
    system = new TestActorSystemConfig() {{
      reaperPeriodMillis = 1;
    }}
    .createActorSystem()
    .on(TARGET).withConfig(new ActorConfig() {{
      reapTimeoutMillis = 600_000;
    }})
    .cue(instrumentedActor(counters))
    .ingress().act(a -> a.to(ActorRef.of(TARGET)).tell());
    
    system.drain(0);

    assertEquals(1, counters.activated.get());
    assertEquals(1, counters.acted.get());

    TestSupport.sleep(200);
    assertEquals(0, counters.passivated.get());
  }
  
  private Supplier<Actor> instrumentedActor(Counters counters) {
    return StatelessLambdaActor
        .builder()
        .activated(a -> {
          log("activating\n");
          counters.activated.incrementAndGet();
        })
        .act((a, m) -> {
          log("act\n");
          counters.acted.incrementAndGet();
        })
        .passivated(a -> {
          log("passivating\n");
          counters.passivated.incrementAndGet();
        });
  }
}
