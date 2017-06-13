package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;

import com.obsidiandynamics.indigo.iot.frame.*;

public final class Session {
  private final long connectTime = System.currentTimeMillis();
  
  private volatile String sessionId;
  
  private volatile Auth auth;
  
  private volatile Subscription subscription = () -> Collections.emptySet();
  
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
  public <A extends Auth> A getAuth() {
    return (A) auth;
  }
  
  void setAuth(Auth auth) {
    this.auth = auth;
  }
  
  @SuppressWarnings("unchecked")
  <S extends Subscription> S getSubscription() {
    return (S) subscription;
  }

  void setSubscription(Subscription subscription) {
    this.subscription = subscription;
  }
}
