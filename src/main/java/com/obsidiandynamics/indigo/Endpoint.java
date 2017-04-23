package com.obsidiandynamics.indigo;

@FunctionalInterface
public interface Endpoint {
  void send(Message message);
}
