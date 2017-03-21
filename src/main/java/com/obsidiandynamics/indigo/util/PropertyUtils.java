package com.obsidiandynamics.indigo.util;

import java.util.function.*;

public final class PropertyUtils {
  private PropertyUtils() {}
  
  public static <T> T get(String key, Function<String, T> parser, T defaultValue) {
    final String str = System.getProperty(key);
    return str != null ? parser.apply(str) : defaultValue;
  }
}
