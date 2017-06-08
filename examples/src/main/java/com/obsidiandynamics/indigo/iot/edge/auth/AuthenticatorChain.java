package com.obsidiandynamics.indigo.iot.edge.auth;

import java.util.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.topic.*;

public final class AuthenticatorChain {
  public static final class NoAuthenticatorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    NoAuthenticatorException(String m) { super(m); }
  }
  
  private final Map<Topic, Authenticator> authenticators = new TreeMap<>(AuthenticatorChain::byLengthDescending);
  
  private static int byLengthDescending(Topic t1, Topic t2) {
    return Integer.compare(t2.length(), t1.length());
  }
  
  private AuthenticatorChain() {}
  
  public static AuthenticatorChain createEmpty() {
    return new AuthenticatorChain();
  }
  
  public static AuthenticatorChain createDefault() {
    return createEmpty().registerDefaults();
  }
  
  private AuthenticatorChain registerDefaults() {
    set("#", Authenticator::allowAll);
    set(Flywheel.REMOTE_PREFIX + "/#", new RemoteTopicAuthenticator());
    return this;
  }
  
  public AuthenticatorChain set(String topicFilter, Authenticator authenticator) {
    authenticators.put(Topic.of(topicFilter), authenticator);
    return this;
  }
  
  public Authenticator get(String topic) {
    final Topic t = Topic.of(topic);
    for (Map.Entry<Topic, Authenticator> entry : authenticators.entrySet()) {
      if (entry.getKey().accepts(t)) {
        return entry.getValue();
      }
    }
    throw new NoAuthenticatorException("No authenticator for topic " + topic);
  }
}
