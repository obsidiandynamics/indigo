package com.obsidiandynamics.indigo.marketfeed.messagebus;

public interface MessageBus {
  MessagePublisher getPublisher(String topic);
  
  
}
