package com.obsidiandynamics.indigo;

public final class ForwardBuilder {
  private final Activation activation;
  
  private final Message message;
  
  ForwardBuilder(Activation activation, Message message) { 
    this.activation = activation;
    this.message = message; 
  }
  
  public void to(ActorRef to) {
    activation.send(new Message(message.from(), to, message.body(), message.requestId(), message.isResponse()));
  }
}