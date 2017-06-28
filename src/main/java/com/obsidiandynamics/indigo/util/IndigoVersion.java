package com.obsidiandynamics.indigo.util;

import java.io.*;
import java.net.*;

public final class IndigoVersion {
  private IndigoVersion() {}
  
  public static String get() throws IOException {
    return get("indigo.version") + "_" + get("indigo.build");
  }
  
  static String get(String versionFile) throws IOException {
    return readResourceHead(versionFile);
  }
  
  private static String readResourceHead(String file) throws IOException {
    final URL url = IndigoVersion.class.getClassLoader().getResource(file);
    if (url == null) throw new FileNotFoundException("resource not found");
    
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      return reader.readLine().trim();
    }
  }
}
