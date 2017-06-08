package com.obsidiandynamics.indigo.iot.rig;

import com.google.gson.*;

abstract class RigSubframe {
  @Override
  public abstract String toString();
  
  final String marshal(Gson gson) {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append(' ');
    sb.append(gson.toJson(this));
    return sb.toString();
  }
  
  @SuppressWarnings("unchecked")
  static final <R extends RigSubframe> R unmarshal(String str, Gson gson) {
    final int separatorIdx = str.indexOf(' ');
    final String className = str.substring(0, separatorIdx);
    final String body = str.substring(separatorIdx + 1);
    try {
      final Class<?> cls = Class.forName(className);
      return (R) gson.fromJson(body, cls);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
