package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

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
  
  boolean isFault() {
    return body instanceof Fault;
  }

  @SuppressWarnings("unchecked")
  public <T> T body() {
    return (T) body;
  }
  
  public static final class MessageBuilder {
    private ActorRef from, to;
    private Object body;
    
    public MessageBuilder from(ActorRef from) { this.from = from; return this; }
    public MessageBuilder to(ActorRef to) { this.to = to; return this; }
    public MessageBuilder body(Object body) { this.body = body; return this; }
    
    public Message build() {
      if (to == null) throw new IllegalArgumentException("Destination not specified");
      return new Message(from, to, body, null, false);
    }
  }
  
  public static MessageBuilder builder() {
    return new MessageBuilder();
  }
  
  public final class SwitchBuilder {
    private boolean consumed;
    
    public final class ThenAction<B> {
      private final Class<B> bodyClass;
      
      ThenAction(Class<B> bodyClass) { this.bodyClass = bodyClass; }
      
      public SwitchBuilder then(Consumer<B> bodyConsumer) {
        if (! consumed && body != null && bodyClass.isAssignableFrom(body.getClass())) {
          bodyConsumer.accept(body());
          consumed = true;
        }
        return SwitchBuilder.this;
      }
    }
    
    public SwitchBuilder whenNull(Consumer<?> nullBodyConsumer) {
      return whenNull(() -> nullBodyConsumer.accept(null));
    }
    
    public SwitchBuilder whenNull(Runnable action) {
      if (! consumed && body == null) {
        action.run();
        consumed = true;
      }
      return SwitchBuilder.this;
    }
    
    public <B> ThenAction<B> when(Class<B> bodyClass) {
      return new ThenAction<>(bodyClass);
    }
    
    public <B> void otherwise(Consumer<B> bodyConsumer) {
      if (! consumed) {
        bodyConsumer.accept(body());
        consumed = true;
      }
    }
  }
  
  public SwitchBuilder switchBody() {
    return new SwitchBuilder();
  }

  @Override
  public String toString() {
    return "Message [from=" + from + ", to=" + to + ", body=" + body + 
        (requestId != null ? ", requestId=" + requestId + ", response=" + response : "") + "]";
  }
}
