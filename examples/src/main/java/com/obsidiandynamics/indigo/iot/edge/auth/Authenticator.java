package com.obsidiandynamics.indigo.iot.edge.auth;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;

public interface Authenticator {
  public interface AuthenticationOutcome {
    void allow();
    
    void deny(TopicAccessError error);
  }
  
  void verify(EdgeNexus nexus, Auth auth, String topic, AuthenticationOutcome outcome);
  
  static void allowAll(EdgeNexus nexus, Auth auth, String topic, AuthenticationOutcome outcome) {
    outcome.allow();
  }
}
