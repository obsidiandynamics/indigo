package com.obsidiandynamics.indigo.messagebus;

import com.google.gson.*;

public final class GsonCodec implements MessageCodec {
  private final Gson gson;
  
  public GsonCodec(Gson gson) {
    this.gson = gson;
  }
  
  @Override
  public String encode(Object obj) {
    if (obj != null) {
      final String className = obj.getClass().getName();
      final String json = gson.toJson(obj);
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
        return gson.fromJson(json, cls);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      return null;
    }
  }
}
