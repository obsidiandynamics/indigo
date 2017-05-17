package com.obsidiandynamics.indigo.messagebus;

public final class StringCodec implements MessageCodec {
  @Override
  public String encode(Object obj) {
    return String.valueOf(obj);
  }

  @Override
  public Object decode(String str) {
    return str.equals("null") ? null : str;
  }
}
