package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;

import java.io.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public final class JvmVersionProviderTest implements TestSupport {
  @Test
  public void testDefault() {
    final JvmVersion version = new JvmVersionProvider.DefaultProvider().get();
    assertTrue(version.major >= 1);
    assertTrue(version.minor >= 8);
    assertTrue(version.update >= 0);
    assertTrue(version.build >= 1);
  }
  
  @Test
  public void testFallback() throws IOException {
    synchronized (System.class) {
      // as we're tinkering with System.err, which is a singleton, only one test can be allowed to proceed per class loader
      final PrintStream standardErr = System.err;
      
      try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintStream customErr = new PrintStream(out)) {
        System.setErr(customErr);
        final JvmVersion version = new JvmVersionProvider.DefaultProvider("some.faulty.version").get();
        assertEquals(1, version.major);
        assertEquals(8, version.minor);
        assertEquals(0, version.update);
        assertEquals(1, version.build);
        
        customErr.flush();
        final String output = new String(out.toByteArray());
        log("output is %s\n", output);
        assertTrue(output.startsWith("WARNING"));
      } finally {
        System.setErr(standardErr);
      }
    }
  }
}
