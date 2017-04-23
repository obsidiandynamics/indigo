package com.obsidiandynamics.indigo;

import java.util.*;

final class TimeoutTask {
  private final long expiresAt;
  
  private final UUID requestId;
  
  private final ActorRef actorRef;
  
  private final PendingRequest request;

  TimeoutTask(long expiresAt, UUID requestId, ActorRef actorRef, PendingRequest request) {
    this.expiresAt = expiresAt;
    this.requestId = requestId;
    this.actorRef = actorRef;
    this.request = request;
  }
  
  long getExpiresAt() {
    return expiresAt;
  }

  UUID getRequestId() {
    return requestId;
  }

  ActorRef getActorRef() {
    return actorRef;
  }
  
  PendingRequest getRequest() {
    return request;
  }
  
  static int byExpiry(TimeoutTask t1, TimeoutTask t2) {
    final int expiresComp = Long.compare(t1.expiresAt, t2.expiresAt);
    return expiresComp != 0 ? expiresComp : t1.requestId.compareTo(t2.requestId);
  }

  @Override
  public String toString() {
    return "TimeoutTask [expiresAt=" + expiresAt + ", requestId=" + requestId + ", actorRef=" + actorRef
           + ", request=" + request + "]";
  }
}