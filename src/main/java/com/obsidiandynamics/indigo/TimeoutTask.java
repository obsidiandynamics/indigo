package com.obsidiandynamics.indigo;

import java.util.*;

import com.obsidiandynamics.indigo.task.*;

final class TimeoutTask extends Task<UUID> {
  private final ActorRef actorRef;
  
  private final Endpoint endpoint;
  
  TimeoutTask(long expiresAt, UUID requestId, ActorRef actorRef, Endpoint endpoint) {
    super(expiresAt, requestId);
    this.actorRef = actorRef;
    this.endpoint = endpoint;
  }
  
  @Override
  protected void execute() {
    endpoint.send(new Message(null, actorRef, Timeout.instance(), getId(), true));
  }
  
  @Override
  public String toString() {
    return TimeoutTask.class.getSimpleName() + " [time=" + getTime() + ", id=" + getId() + ", actorRef=" + actorRef + "]";
  }
}