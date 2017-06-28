package com.obsidiandynamics.indigo.util;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

public class IndigoVersionTest implements TestSupport {
  @Test
  public void testValid() throws IOException {
    final String version = IndigoVersion.get();
    assertNotNull(version);
    assertTrue(version.contains("_"));
  }
  
  @Test(expected=IOException.class)
  public void testInvalid() throws IOException {
    IndigoVersion.get("wrong.file");
  }
}
