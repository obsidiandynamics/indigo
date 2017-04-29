package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static junit.framework.TestCase.*;

import org.junit.*;

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
  public void testUnsupportedSignal() throws InterruptedException {
    system.on(SINK).cue((a, m) -> {
      a.reply(m).tell(new BadSignal());
    })
    .ingress(a -> { 
      a.to(ActorRef.of(SINK)).ask()
      .onFault(f -> {
        fail("Unexpected fault");
      })
      .await(10_000).onTimeout(() -> {
        fail("Unexpected timeout");
      })
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
