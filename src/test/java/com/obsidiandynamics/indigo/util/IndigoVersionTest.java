package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public class IndigoVersionTest implements TestSupport {
  @Test
  public void testValid() {
    assertNotNull(IndigoVersion.get());
  }
  @Test
  public void testInvalid() throws IOException {
    synchronized (System.class) {
      // as we're tinkering with System.err, which is a singleton, only one test can be allowed to proceed per class loader
      final PrintStream standardErr = System.err;
      try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintStream customErr = new PrintStream(out)) {
        System.setErr(customErr);

        assertNull(IndigoVersion.get("wrong.file"));
        
        customErr.flush();
        final String output = new String(out.toByteArray());
        log("output is %s\n", output);
        assertTrue("output=" + output, output.startsWith("I/O error"));
      } finally {
        System.setErr(standardErr);
      }
    }
  }
}
