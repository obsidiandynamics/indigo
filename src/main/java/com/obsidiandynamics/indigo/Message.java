package com.obsidiandynamics.indigo;

import java.util.*;

public final class Message {
  private final ActorRef from;
  
  private final ActorRef to;
  
  private final Object body;
  
  private final UUID requestId;

  Message(ActorRef from, ActorRef to, Object body, UUID requestId) {
    this.from = from;
    this.to = to;
    this.body = body;
    this.requestId = requestId;
  }
  
  public ActorRef from() {
    return from;
  }
  
  public ActorRef to() {
    return to;
  }

  public UUID requestId() {
    return requestId;
  }

  @SuppressWarnings("unchecked")
  public <T> T body() {
    return (T) body;
  }

  @Override
  public String toString() {
    return "Message [from=" + from + ", to=" + to + ", body=" + body + 
        (requestId != null ? ", requestId=" + requestId : "") + "]";
  }
}
