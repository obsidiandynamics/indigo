package com.obsidiandynamics.indigo.iot.rig;

import com.google.gson.*;

abstract class RigSubframe {
  static final String TOPIC_PREFIX = "$remote";
  
  private static final String SYNC_TYPE = "sync";
  
  @Override
  public abstract String toString();
  
  final String marshal(Gson gson) {
    final StringBuilder sb = new StringBuilder();
//    if (this instanceof Sync) {
//      sb.append(SYNC_TYPE);
//      sb.append(' ');
//      sb.append(((Sync) this).getNanoTime());
//    } else {
      sb.append(getClass().getName());
      sb.append(' ');
      sb.append(gson.toJson(this));
//    }
    return sb.toString();
  }
  
  @SuppressWarnings("unchecked")
  static final <R extends RigSubframe> R unmarshal(String str, Gson gson) {
    final int separatorIdx = str.indexOf(' ');
    final String className = str.substring(0, separatorIdx);
    final String body = str.substring(separatorIdx + 1);
//    if (className.equals(SYNC_TYPE)) {
//      return (R) new Sync(Long.valueOf(body));
//    } else {
      try {
        final Class<?> cls = Class.forName(className);
        return (R) gson.fromJson(body, cls);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
//    }
  }
}
