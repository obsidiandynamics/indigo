package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class IngressBuilder {
  private final ActorSystem system;
  
  private int iterations = 1;
  
  IngressBuilder(ActorSystem system) {
    this.system = system;
  }
  
  public IngressBuilder times(int iterations) {
    this.iterations = iterations;
    return this;
  }
  
  public ActorSystem act(Consumer<Activation> act) {
    return act((a, i) -> act.accept(a));
  }
  
  public ActorSystem act(BiConsumer<Activation, Integer> act) {
    for (int i = 0; i < iterations; i++) {
      final int _i = i;
      system.ingress(a -> act.accept(a, _i));
    }
    return system;
  }
}