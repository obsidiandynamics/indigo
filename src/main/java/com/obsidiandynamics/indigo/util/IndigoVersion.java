package com.obsidiandynamics.indigo.util;

import java.io.*;

import com.obsidiandynamics.version.*;

public final class IndigoVersion {
  private IndigoVersion() {}
  
  public static String get() throws IOException {
    return AppVersion.get("indigo");
  }
}
