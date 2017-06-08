package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;

public final class Session {
  private final long connectTime = System.currentTimeMillis();
  
  private String sessionId;
  
  private Subscription subscription = () -> Collections.emptySet();
  
  Session() {}
  
  public long getConnectTime() {
    return connectTime;
  }

  public String getSessionId() {
    return sessionId;
  }
  
  void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
  
  @SuppressWarnings("unchecked")
  <S extends Subscription> S getSubscription() {
    return (S) subscription;
  }

  void setSubscription(Subscription subscription) {
    this.subscription = subscription;
  }
}
