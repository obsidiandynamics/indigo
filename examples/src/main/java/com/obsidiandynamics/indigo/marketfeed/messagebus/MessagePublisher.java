package com.obsidiandynamics.indigo.marketfeed.messagebus;

public interface MessagePublisher {
  void send(Object message);
  
  void close();
}
