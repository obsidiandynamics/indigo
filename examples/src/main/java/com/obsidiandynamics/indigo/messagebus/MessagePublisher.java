package com.obsidiandynamics.indigo.messagebus;

public interface MessagePublisher extends AutoCloseable {
  void send(Object message);
  
  @Override
  void close();
}
