package com.obsidiandynamics.indigo.messagebus;

public interface MessagePublisher {
  void send(Object message);
  
  void close();
}
