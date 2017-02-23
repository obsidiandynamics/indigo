package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class LambdaActor extends Actor {
  private Consumer<Activation> consumer;
  
  LambdaActor(Consumer<Activation> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void act(Activation a) {
    consumer.accept(a);
  }
}
