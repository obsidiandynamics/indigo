package com.obsidiandynamics.indigo.messagebus;

public interface MessagePublisher extends SafeCloseable {
  void send(Object message);
}
