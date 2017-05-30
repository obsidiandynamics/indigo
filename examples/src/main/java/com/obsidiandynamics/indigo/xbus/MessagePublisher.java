package com.obsidiandynamics.indigo.xbus;

public interface MessagePublisher extends SafeCloseable {
  void send(Object message);
}
