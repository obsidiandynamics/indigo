package com.obsidiandynamics.indigo;

import java.util.*;

public final class Message {
  private final ActorRef from;
  
  private final ActorRef to;
  
  private final Object body;
  
  private final UUID requestId;
  
  private final boolean response;

  Message(ActorRef from, ActorRef to, Object body, UUID requestId, boolean response) {
    this.from = from;
    this.to = to;
    this.body = body;
    this.requestId = requestId;
    this.response = response;
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
  
  public boolean isResponse() {
    return response;
  }

  @SuppressWarnings("unchecked")
  public <T> T body() {
    return (T) body;
  }

  @Override
  public String toString() {
    return "Message [from=" + from + ", to=" + to + ", body=" + body + 
        (requestId != null ? ", requestId=" + requestId + ", response=" + response : "") + "]";
  }
}
