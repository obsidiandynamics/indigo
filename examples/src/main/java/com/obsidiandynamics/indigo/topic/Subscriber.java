package com.obsidiandynamics.indigo.topic;

public interface Subscriber {
  void accept(Delivery delivery);
}
