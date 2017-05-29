package com.obsidiandynamics.indigo.topic;

@FunctionalInterface
public interface Subscriber {
  void accept(Delivery delivery);
}
