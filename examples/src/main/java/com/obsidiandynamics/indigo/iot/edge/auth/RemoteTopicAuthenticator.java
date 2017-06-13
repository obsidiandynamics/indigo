package com.obsidiandynamics.indigo.iot.edge.auth;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;

public final class RemoteTopicAuthenticator implements Authenticator {
  @Override
  public void verify(EdgeNexus nexus, String topic, AuthenticationOutcome outcome) {
    final String sessionId = nexus.getSession().getSessionId();
    if (sessionId == null) {
      outcome.deny(new TopicAccessError("No session ID", topic));
      return;
    }
    
    final String allowedTopicPrefix = Flywheel.getSessionTopicPrefix(sessionId);
    if (topic.startsWith(allowedTopicPrefix)) {
      outcome.allow();
    } else{
      outcome.deny(new TopicAccessError(String.format("Restricted to %s/#", allowedTopicPrefix), topic));
    }
  }
}
