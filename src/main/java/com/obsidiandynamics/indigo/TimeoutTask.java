package com.obsidiandynamics.indigo;

import java.util.*;

final class TimeoutTask implements Comparable<TimeoutTask> {
  private final long expiresAt;
  
  private final UUID requestId;
  
  private final Activation activation;
  
  private final PendingRequest request;

  TimeoutTask(long expiresAt, UUID requestId, Activation activation, PendingRequest request) {
    this.expiresAt = expiresAt;
    this.requestId = requestId;
    this.activation = activation;
    this.request = request;
  }
  
  long getExpiresAt() {
    return expiresAt;
  }

  UUID getRequestId() {
    return requestId;
  }

  Activation getActivation() {
    return activation;
  }
  
  PendingRequest getRequest() {
    return request;
  }

  @Override
  public int compareTo(TimeoutTask o) {
    final int expiresComp = Long.compare(expiresAt, o.expiresAt);
    return expiresComp != 0 ? expiresComp : requestId.compareTo(o.requestId);
  }

  @Override
  public String toString() {
    return "TimeoutTask [expiresAt=" + expiresAt + ", requestId=" + requestId + ", activation=" + activation
           + ", request=" + request + "]";
  }
}