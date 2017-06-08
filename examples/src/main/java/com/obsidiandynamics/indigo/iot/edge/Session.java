package com.obsidiandynamics.indigo.iot.edge;

public final class Session {
  private final long connectTime = System.currentTimeMillis();
  
  private volatile String sessionId;
  
  private volatile Subscription subscription;

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
