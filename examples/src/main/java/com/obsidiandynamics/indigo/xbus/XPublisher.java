package com.obsidiandynamics.indigo.xbus;

public interface XPublisher extends SafeCloseable {
  void send(Object message);
}
