package com.obsidiandynamics.indigo.messagebus;

public interface MessageSubscriber {
  Object receive();
  
  void close();
}
