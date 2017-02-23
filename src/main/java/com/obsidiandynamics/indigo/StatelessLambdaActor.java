package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatelessLambdaActor extends Actor {
  private final Consumer<Activation> consumer;
  
  StatelessLambdaActor(Consumer<Activation> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void act(Activation a) {
    consumer.accept(a);
  }
}
