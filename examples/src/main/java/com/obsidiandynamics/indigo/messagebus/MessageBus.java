package com.obsidiandynamics.indigo.messagebus;

public interface MessageBus extends SafeCloseable {
  MessagePublisher getPublisher(String topic);
  
  MessageSubscriber getSubscriber(String topic);
}
