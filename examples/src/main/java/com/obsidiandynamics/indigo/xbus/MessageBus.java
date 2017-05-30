package com.obsidiandynamics.indigo.xbus;

public interface MessageBus extends SafeCloseable {
  MessagePublisher getPublisher(String topic);
  
  MessageSubscriber getSubscriber(String topic);
}
