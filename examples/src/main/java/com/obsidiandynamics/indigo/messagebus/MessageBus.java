package com.obsidiandynamics.indigo.messagebus;

public interface MessageBus {
  MessagePublisher getPublisher(String topic);
  
  MessageSubscriber getSubscriber(String topic);
}
