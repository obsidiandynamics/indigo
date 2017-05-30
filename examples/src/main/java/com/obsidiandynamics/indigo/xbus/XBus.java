package com.obsidiandynamics.indigo.xbus;

public interface XBus extends SafeCloseable {
  XPublisher getPublisher(String topic);
  
  XSubscriber getSubscriber(String topic);
}
