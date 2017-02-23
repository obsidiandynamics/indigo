package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatefulLambdaActor<S> extends Actor {
  private final BiConsumer<Activation, S> consumer;
  private final S state;
  
  StatefulLambdaActor(BiConsumer<Activation, S> consumer, S state) {
    this.consumer = consumer;
    this.state = state;
  }

  @Override
  public void act(Activation a) {
    consumer.accept(a, state);
  }
}
