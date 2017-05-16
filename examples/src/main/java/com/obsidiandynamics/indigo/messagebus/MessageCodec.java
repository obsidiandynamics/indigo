package com.obsidiandynamics.indigo.messagebus;

public interface MessageCodec {
  String encode(Object obj);
  
  Object decode(String str);
}
