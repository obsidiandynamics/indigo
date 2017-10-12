package com.obsidiandynamics.indigo.util;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

public class IndigoVersionTest implements TestSupport {
  @Test
  public void testValid() throws IOException {
    assertNotNull(IndigoVersion.get());
  }
}
