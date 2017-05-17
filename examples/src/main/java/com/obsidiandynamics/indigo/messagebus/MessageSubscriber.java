package com.obsidiandynamics.indigo.messagebus;

public interface MessageSubscriber extends AutoCloseable {
  Object receive();
  
  @Override
  void close();
}
