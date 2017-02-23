package com.obsidiandynamics.indigo;

public final class Message {
  private final ActorId to;
  
  private final Object body;

  public Message(ActorId to, Object body) {
    this.to = to;
    this.body = body;
  }
  
  public ActorId to() {
    return to;
  }

  public Object body() {
    return body;
  }

  @Override
  public String toString() {
    return "Message [to=" + to + ", body=" + body + "]";
  }
}
