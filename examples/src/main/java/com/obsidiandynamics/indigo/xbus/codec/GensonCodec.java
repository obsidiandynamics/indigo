package com.obsidiandynamics.indigo.xbus.codec;

import com.owlike.genson.*;

public final class GensonCodec implements MessageCodec {
  private final Genson genson;
  
  public GensonCodec(Genson genson) {
    this.genson = genson;
  }

  @Override
  public String encode(Object obj) {
    if (obj != null) {
      final String className = obj.getClass().getName();
      final String json = genson.serialize(obj);
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
        return genson.deserialize(json, cls);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      return null;
    }
  }
}
