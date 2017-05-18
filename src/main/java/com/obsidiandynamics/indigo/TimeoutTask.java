package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.task.*;

final class TimeoutTask extends Task<UUID> {
  private final ActorRef actorRef;
  
  private final PendingRequest request;
  
  private final Endpoint endpoint;
  
  private final StackTraceElement[] stack; //TODO remove

  TimeoutTask(long expiresAt, UUID requestId, ActorRef actorRef, PendingRequest request, Endpoint endpoint) {
    super(expiresAt, requestId);
    this.actorRef = actorRef;
    this.request = request;
    this.endpoint = endpoint;
    stack = Thread.currentThread().getStackTrace();
  }
  
  @Override
  protected void execute() {
    try {
      endpoint.send(new Message(null, actorRef, Timeout.instance(), getId(), true));
    } catch (RejectedExecutionException e) {
      System.err.println("TimeoutTask.execute");
      System.err.println(Arrays.asList(stack).toString().replace(',', '\n'));
      throw e;
    }
  }
  
  @Override
  public String toString() {
    return "TimeoutTask [time=" + getTime() + ", id=" + getId() + ", actorRef=" + actorRef
           + ", request=" + request + "]";
  }
}