package com.obsidiandynamics.indigo.messagebus;

import com.owlike.genson.*;

public final class GensonCodec implements MessageCodec {
  private static Genson createGenson() {
    return new GensonBuilder().useClassMetadata(true).useIndentation(true).create();
  }
 
  @Override
  public String encode(Object obj) {
    if (obj != null) {
      final String className = obj.getClass().getName();
      final String json = createGenson().serialize(obj);
      return className + " " + json;
    } else {
      return "";
    }
  }

  @Override
  public Object decode(String str) {
    if (! str.isEmpty()) {
      final int separatorIdx = str.indexOf(' ');
      final String className = str.substring(0, separatorIdx);
      try {
        final Class<?> cls = Class.forName(className);
        final String json = str.substring(separatorIdx + 1);
        return createGenson().deserialize(json, cls);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      return null;
    }
  }
}
