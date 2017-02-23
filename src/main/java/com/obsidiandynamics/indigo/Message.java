package com.obsidiandynamics.indigo;

public final class Message {
  private final ActorId from;
  
  private final ActorId to;
  
  private final Object body;

  Message(ActorId from, ActorId to, Object body) {
    this.from = from;
    this.to = to;
    this.body = body;
  }
  
  public ActorId from() {
    return from;
  }
  
  public ActorId to() {
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
