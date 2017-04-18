package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static junit.framework.TestCase.*;

import java.io.*;
import java.util.function.*;

import org.junit.*;

public final class ExceptionHandlerTest implements TestSupport {
  private static final String SINK = "sink";
  
  @Test(expected=IllegalArgumentException.class)
  public void testSystemSystem() {
    logTestName();
    new TestActorSystemConfig() {{
      exceptionHandler = SYSTEM;
    }}
    .define().shutdownQuietly();
  }
  
  private final class HandlerTestException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
  
  @Test
  public void testSystemConsole() throws IOException {
    testConsole(() -> new ActorSystemConfig() {{
                  exceptionHandler = CONSOLE;
                }},
                () -> new ActorConfig() {{
                  exceptionHandler = SYSTEM;
                }});
  }
  
  @Test
  public void testActorConsole() throws IOException {
    testConsole(() -> new ActorSystemConfig() {{
                  exceptionHandler = (as, t) -> {};
                }},
                () -> new ActorConfig() {{
                  exceptionHandler = CONSOLE;
                }});
  }
  
  private void testConsole(Supplier<ActorSystemConfig> actorSystemConfigSupplier, Supplier<ActorConfig> actorConfigSupplier) throws IOException {
    synchronized (ExceptionHandlerTest.class) {
      // as we're tinkering with System.err, which is a singleton, only one test can be allowed to proceed per class loader
      logTestName();
      
      final PrintStream standardErr = System.err;
      try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintStream customErr = new PrintStream(out)) {
        System.setErr(customErr);
        
        assertEquals(0, out.size());
        
        actorSystemConfigSupplier.get().define()
        .when(SINK).configure(actorConfigSupplier.get()).lambda((a, m) -> {
          throw new HandlerTestException();
        })
        .ingress(a -> a.to(ActorRef.of(SINK)).tell())
        .shutdownQuietly();
        
        customErr.flush();
        final String output = new String(out.toByteArray());
        log("output is %s\n", output);
        
        final String exceptionFullName = HandlerTestException.class.getName();
        assertTrue(output.startsWith(exceptionFullName));
      } finally {
        System.setErr(standardErr);
      }
    }
  }
  
  @Test(expected=UnhandledMultiException.class)
  public void testDrain() throws InterruptedException {
    final ActorSystem system = new TestActorSystemConfig() {{
      exceptionHandler = DRAIN;
    }}
    .define()
    .when(SINK).lambda((a, m) -> {
      throw new HandlerTestException();
    })
    .ingress(a -> a.to(ActorRef.of(SINK)).tell());
    
    try {
      system.drain(0);
    } finally {
      system.shutdownQuietly();
    }
  }
}
