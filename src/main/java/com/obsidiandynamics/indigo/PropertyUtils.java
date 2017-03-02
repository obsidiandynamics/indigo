package com.obsidiandynamics.indigo;

import java.util.function.*;

final class PropertyUtils {
  private PropertyUtils() {}
  
  static <T> T get(String key, Function<String, T> parser, T defaultValue) {
    final String str = System.getProperty(key);
    return str != null ? parser.apply(str) : defaultValue;
  }
}
