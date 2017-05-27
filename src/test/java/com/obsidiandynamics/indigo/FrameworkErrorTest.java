package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static junit.framework.TestCase.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class FrameworkErrorTest implements TestSupport {
  private static final String SINK = "sink";
  
  private static final class BadSignal implements Signal {}
  
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {{
      exceptionHandler = DRAIN;
    }}
    .createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test
  public void testUnsupportedSolicitedSignal() throws InterruptedException {
    system.on(SINK).cue((a, m) -> {
      a.reply(m).tell(new BadSignal());
    })
    .ingress(a -> { 
      a.to(ActorRef.of(SINK)).ask()
      .onFault(f -> {
        fail("Unexpected fault");
      })
      .await(60_000).onTimeout(() -> { /* may still time out when draining the schedulers during system termination */ })
      .onResponse(r -> {
        fail("Unexpected response");
      });
    });
    
    try {
      system.drain(0);
      fail("Failed to catch UnhandledMultiException");
    } catch (UnhandledMultiException e) {
      assertEquals(1, e.getErrors().length);
      assertFrameworkError(e.getErrors()[0]);
    }
    
    assertFalse(system.isRunning());
  }
  
  @Test
  public void testUnsupportedUnsolicitedSignal() throws InterruptedException {
    system.ingress(a -> { 
      a.to(ActorRef.of(ActorRef.INGRESS)).ask(new BadSignal())
      .onFault(f -> {
        fail("Unexpected fault");
      })
      .await(60_000).onTimeout(() -> { /* may still time out when draining the schedulers during system termination */ })
      .onResponse(r -> {
        fail("Unexpected response");
      });
    });
    
    try {
      system.drain(0);
      fail("Failed to catch UnhandledMultiException");
    } catch (UnhandledMultiException e) {
      assertEquals(1, e.getErrors().length);
      assertFrameworkError(e.getErrors()[0]);
    }
    
    assertFalse(system.isRunning());
  }
  
  private void assertFrameworkError(Throwable t) {
    assertEquals(FrameworkError.class, t.getClass());
    assertEquals("Unsupported signal of type " + BadSignal.class.getName(), t.getMessage());
  }
}
