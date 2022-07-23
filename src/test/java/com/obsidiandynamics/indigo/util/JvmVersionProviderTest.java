package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;

import java.io.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

import nl.jqno.equalsverifier.*;

public final class JvmVersionProviderTest implements TestSupport {
  @Test
  public void testDefault() {
    final JvmVersion version = new JvmVersionProvider.DefaultProvider().get();
    assertTrue("version=" + version, version.major >= 1);
    assertTrue("version=" + version, version.minor >= 0);
    assertTrue("version=" + version, version.update >= 0);
    assertTrue("version=" + version, version.build >= 0);
  }
  
  @Test
  public void testJvmVersion_equalsHashCode() {
    EqualsVerifier.forClass(JvmVersion.class).verify();
  }
  
  @Test
  public void testCustomFourSegments() {
    final JvmVersion version = new JvmVersionProvider.DefaultProvider("1.8.0_101").get();
    assertEquals(1, version.major);
    assertEquals(8, version.minor);
    assertEquals(0, version.update);
    assertEquals(101, version.build);
  }
  
  @Test
  public void testCustomThreeSegments() {
    final JvmVersion version = new JvmVersionProvider.DefaultProvider("10.0.1").get();
    assertEquals(10, version.major);
    assertEquals(0, version.minor);
    assertEquals(1, version.update);
    assertEquals(0, version.build);
  }
  
  @Test
  public void testCustomTwoSegments() {
    final JvmVersion version = new JvmVersionProvider.DefaultProvider("10.1").get();
    assertEquals(10, version.major);
    assertEquals(1, version.minor);
    assertEquals(0, version.update);
    assertEquals(0, version.build);
  }
  
  @Test
  public void testCustomOneSegment() {
    final JvmVersion version = new JvmVersionProvider.DefaultProvider("10").get();
    assertEquals(10, version.major);
    assertEquals(0, version.minor);
    assertEquals(0, version.update);
    assertEquals(0, version.build);
  }
  
  @Test
  public void testFallback() throws IOException {
    final String output;
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
        output = out.toString();
        log("output is %s\n", output);
        assertTrue("output=" + output, output.startsWith("WARNING"));
      } finally {
        System.setErr(standardErr);
      }
    }
    
    log("output is %s\n", output);
    assertTrue("output=<<" + output + ">>", output.startsWith("WARNING"));
  }
  
  @Test
  public void compareVersions() {
    assertEquals(-1, compare("1.8.0_1", "2.0.0_1"));
    assertEquals(-1, compare("1.8.0_1", "1.9.0_1"));
    assertEquals(-1, compare("1.8.0_1", "1.8.1_1"));
    assertEquals(-1, compare("1.8.0_1", "1.8.0_2"));
  }
  
  private static int compare(String v1, String v2) {
    final JvmVersion j1 = new JvmVersionProvider.DefaultProvider(v1).get();
    final JvmVersion j2 = new JvmVersionProvider.DefaultProvider(v2).get();
    return j1.compareTo(j2);
  }
}
