package com.obsidiandynamics.indigo.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;

public final class PropertyUtils {
  private PropertyUtils() {}
  
  public static <T> T get(String key, Function<String, T> parser, T defaultValue) {
    return get(System.getProperties(), key, parser, defaultValue);
  }
  
  public static <T> T get(Hashtable<?, ?> props, String key, Function<String, T> parser, T defaultValue) {
    final String str = (String) props.get(key);
    return str != null ? parser.apply(str) : defaultValue;
  }
  
  public static <T> T getOrSet(Hashtable<Object, Object> props, String key, Function<String, T> parser, T defaultValue) {
    final String str = (String) props.get(key);
    if (str == null) {
      props.put(key, defaultValue);
      return defaultValue;
    } else {
      return parser.apply(str);
    }
  }
  
  public static Properties load(String resourceFile) throws IOException {
    final URL url = PropertyUtils.class.getClassLoader().getResource(resourceFile);
    if (url == null) throw new FileNotFoundException("resource not found");
    
    final Properties props = new Properties();
    try (InputStream in = url.openStream()) {
      props.load(in);
    }
    return props;
  }
  
  public static Properties load(String resourceFile, Properties defaultHashtable) {
    try {
      return load(resourceFile);
    } catch (IOException e) {
      return defaultHashtable;
    }
  }
  
  public static Properties filter(String keyPrefix, Hashtable<Object, Object> props) {
    final Properties filtered = new Properties();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      if (((String) entry.getKey()).startsWith(keyPrefix)) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }
    return filtered;
  }
}
