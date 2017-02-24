package com.obsidiandynamics.indigo;

public final class Message {
  private final ActorRef from;
  
  private final ActorRef to;
  
  private final Object body;

  Message(ActorRef from, ActorRef to, Object body) {
    this.from = from;
    this.to = to;
    this.body = body;
  }
  
  public ActorRef from() {
    return from;
  }
  
  public ActorRef to() {
    return to;
  }

  @SuppressWarnings("unchecked")
  public <T> T body() {
    return (T) body;
  }

  @Override
  public String toString() {
    return "Message [from=" + from + ", to=" + to + ", body=" + body + "]";
  }
}
