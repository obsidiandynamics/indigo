package com.obsidiandynamics.indigo.util;

import java.io.*;
import java.net.*;

public final class IndigoVersion {
  private IndigoVersion() {}
  
  public static String get() {
    return get("indigo.version");
  }
  
  static String get(String versionFile) {
    try {
      return readResourceHead(versionFile);
    } catch (IOException e) {
      System.err.format("I/O error reading %s: %s", versionFile, e);
      return null;
    }
  }
  
  private static String readResourceHead(String file) throws IOException {
    final URL url = IndigoVersion.class.getClassLoader().getResource(file);
    if (url == null) throw new FileNotFoundException("resource not found");
    
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      return reader.readLine().trim();
    }
  }
}
