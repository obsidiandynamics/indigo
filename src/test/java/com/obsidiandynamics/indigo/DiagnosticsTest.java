package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.io.*;

import org.junit.*;

public final class DiagnosticsTest implements TestSupport {
  @Test
  public void testLimit() throws IOException {
    test(10);
  }

  private void test(int limit) throws IOException {
    final ActorSystem system = new TestActorSystemConfig() {{
      diagnostics = new Diagnostics() {{
        traceEnabled = true;
        logSize = limit;
      }};
    }}
    .createActorSystem()
    .ingress().times(limit * 2).act((a, i) -> {});
    
    system.shutdownQuietly();

    final int logLength = system.getConfig().diagnostics.getLog().length;
    assertTrue("logLength=" + logLength, logLength <= limit);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintStream print = new PrintStream(out)) {
      system.getConfig().diagnostics.print(print);
      print.flush();
      final String output = new String(out.toByteArray());
      log("output is %s\n", output);
      assertTrue("output.length=" + output.length(), output.length() >= 1);
    }
  }
}
