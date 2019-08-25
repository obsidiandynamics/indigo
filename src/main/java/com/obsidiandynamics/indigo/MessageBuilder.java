package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

public class MessageBuilder {
  @FunctionalInterface
  interface MessageTarget {
    void send(Object body, UUID requestId);
  }
  
  protected final Activation activation;
  
  protected MessageTarget target;
  
  private int copies = 1;
  
  private Object requestBody;
  
  private long timeoutMillis;
  
  private Runnable onTimeout;
  
  private Consumer<Fault> onFault;
  
  private TimeoutTask timeoutTask;
  
  MessageBuilder(Activation activation) {
    this.activation = activation;
  }
  
  final MessageBuilder target(MessageTarget target) {
    this.target = target;
    return this;
  }
  
  public final MessageBuilder times(int copies) {
    this.copies = copies;
    return this;
  }
  
  public final void tell(Object body) {
    for (int i = copies; --i >= 0;) {
      target.send(body, null);
    }
  }
  
  public final MessageBuilder ask(Object requestBody) {
    this.requestBody = requestBody;
    return this;
  }
  
  public final MessageBuilder ask() {
    return ask(null);
  }
  
  public final MessageBuilder await(long timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
    return this;
  }
  
  public final MessageBuilder onTimeout(Runnable onTimeout) {
    this.onTimeout = onTimeout;
    return this;
  }
  
  public final MessageBuilder onFault(Consumer<Fault> onFault) {
    this.onFault = onFault;
    return this;
  }
  
  public final void onResponse(Consumer<Message> onResponse) {
    if (timeoutMillis != 0 ^ onTimeout != null) {
      throw new IllegalArgumentException("Only one of the timeout time or handler has been set");
    }
    
    for (int i = copies; --i >= 0;) {
      final UUID requestId = new UUID(activation.getId(), activation.getAndIncrementRequestCounter());
      final PendingRequest req = new PendingRequest(onResponse, onTimeout, onFault);
      target.send(requestBody, requestId);
      activation.pending.put(requestId, req);
      
      if (timeoutMillis != 0) {
        timeoutTask = new TimeoutTask(System.nanoTime() + timeoutMillis * 1_000_000l,
                                      requestId,
                                      activation.ref,
                                      activation.system);
        req.setTimeoutTask(timeoutTask);
        activation.system.getTimeoutScheduler().schedule(timeoutTask);
      }
    }
  }
  
  final TimeoutTask getTimeoutTask() {
    return timeoutTask;
  }
  
  public final void tell() {
    tell(null);
  }
}
