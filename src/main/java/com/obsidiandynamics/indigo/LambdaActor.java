package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class LambdaActor extends Actor {
  private Consumer<Message> consumer;
  
  LambdaActor(Consumer<Message> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void act(Message m) {
    consumer.accept(m);
  }
}
