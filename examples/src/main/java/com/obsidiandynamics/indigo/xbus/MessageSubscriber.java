package com.obsidiandynamics.indigo.xbus;

public interface MessageSubscriber extends SafeCloseable {
  Object receive();
}
