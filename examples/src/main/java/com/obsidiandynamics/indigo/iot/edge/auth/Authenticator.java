package com.obsidiandynamics.indigo.iot.edge.auth;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;

@FunctionalInterface
public interface Authenticator {
  public interface AuthenticationOutcome {
    void allow();
    
    void deny(TopicAccessError error);
    
    default void forbidden(String topic) {
      deny(new TopicAccessError("Forbidden", topic));
    }
  }
  
  void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome);
  
  static void allowAll(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
    outcome.allow();
  }
  
  static void allowLocal(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
    if (nexus.isLocal()) {
      outcome.allow();
    } else {
      outcome.forbidden(topic);
    }
  }
  
  static void denyAll(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
    outcome.forbidden(topic);
  }
}
