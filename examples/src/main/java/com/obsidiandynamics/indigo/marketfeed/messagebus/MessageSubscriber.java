package com.obsidiandynamics.indigo.marketfeed.messagebus;

public interface MessageSubscriber {
  Object receive();
  
  void close();
}
