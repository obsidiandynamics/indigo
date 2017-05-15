package com.obsidiandynamics.indigo;

import java.util.*;

import com.obsidiandynamics.indigo.task.*;

final class TimeoutTask extends Task<UUID> {
  /** A pre-instantiated timeout signal (all timeout signals are the same). */
  private static final Timeout TIMEOUT_SIGNAL = new Timeout();
  
  private final ActorRef actorRef;
  
  private final PendingRequest request;
  
  private final Endpoint endpoint;

  TimeoutTask(long expiresAt, UUID requestId, ActorRef actorRef, PendingRequest request, Endpoint endpoint) {
    super(expiresAt, requestId);
    this.actorRef = actorRef;
    this.request = request;
    this.endpoint = endpoint;
  }
  
  @Override
  protected void execute() {
    endpoint.send(new Message(null, actorRef, TIMEOUT_SIGNAL, getId(), true));
  }
  
  @Override
  public String toString() {
    return "TimeoutTask [time=" + getTime() + ", id=" + getId() + ", actorRef=" + actorRef
           + ", request=" + request + "]";
  }
}