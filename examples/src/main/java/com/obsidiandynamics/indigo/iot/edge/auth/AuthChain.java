package com.obsidiandynamics.indigo.iot.edge.auth;

import java.util.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.topic.*;

public final class AuthChain {
  public static final class NoAuthenticatorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    NoAuthenticatorException(String m) { super(m); }
  }
  
  private final Map<Topic, Authenticator> authenticators = new TreeMap<>(AuthChain::byLengthDescending);
  
  private static int byLengthDescending(Topic t1, Topic t2) {
    return Integer.compare(t2.length(), t1.length());
  }
  
  private AuthChain() {}
  
  public static AuthChain createDefault() {
    return new AuthChain().registerDefaults();
  }
  
  public AuthChain clear() {
    authenticators.clear();
    return this;
  }
  
  private AuthChain registerDefaults() {
    set("#", Authenticator::allowAll);
    set(Flywheel.REMOTE_PREFIX + "/#", new RemoteTopicAuthenticator());
    return this;
  }
  
  public AuthChain set(String topicFilter, Authenticator authenticator) {
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
  
  public void validate() {
    get("#");
  }
}
