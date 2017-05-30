package com.obsidiandynamics.indigo.xbus.codec;

public interface MessageCodec {
  String encode(Object obj);
  
  Object decode(String str);
}
