package com.obsidiandynamics.indigo.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;

public final class PropertyUtils {
  private PropertyUtils() {}
  
  public static <T> T get(String key, Function<String, T> parser, T defaultValue) {
    final String str = System.getProperties().getProperty(key);
    return str != null ? parser.apply(str) : defaultValue;
  }
  
  public static <T> T get(Properties props, String key, Function<String, T> parser, T defaultValue) {
    final String str = props.getProperty(key);
    return str != null ? parser.apply(str) : defaultValue;
  }
  
  public static Properties load(String resourceFile) throws IOException {
    final URL url = IndigoVersion.class.getClassLoader().getResource(resourceFile);
    if (url == null) throw new FileNotFoundException("resource not found");
    
    final Properties props = new Properties();
    try (InputStream in = url.openStream()) {
      props.load(in);
    }
    return props;
  }
  
  public static Properties load(String resourceFile, Properties defaultProperties) {
    try {
      return load(resourceFile);
    } catch (IOException e) {
      return defaultProperties;
    }
  }
}
