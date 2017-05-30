package com.obsidiandynamics.indigo.xbus;

public interface XSubscriber extends SafeCloseable {
  void send(Object message);
  Object receive();
}
