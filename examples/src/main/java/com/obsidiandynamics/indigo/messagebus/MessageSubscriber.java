package com.obsidiandynamics.indigo.messagebus;

public interface MessageSubscriber extends SafeCloseable {
  Object receive();
}
