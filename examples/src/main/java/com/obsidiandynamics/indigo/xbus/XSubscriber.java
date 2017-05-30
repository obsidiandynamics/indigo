package com.obsidiandynamics.indigo.xbus;

public interface XSubscriber extends SafeCloseable {
  Object receive();
}
