package com.obsidiandynamics.indigo.messagebus.codec;

public interface MessageCodec {
  String encode(Object obj);
  
  Object decode(String str);
}
