package com.obsidiandynamics.indigo;

public final class Message {
  private final ActorId to;
  
  private final Object body;

  Message(ActorId to, Object body) {
    this.to = to;
    this.body = body;
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
    return "Message [to=" + to + ", body=" + body + "]";
  }
}
