package com.obsidiandynamics.indigo.iot;

public interface Flywheel {
  final String REMOTE_PREFIX = "$remote";
  
  static String getSessionTopicPrefix(String sessionId) {
    return REMOTE_PREFIX + "/" + sessionId;
  }
  
  static String getRxTopicPrefix(String sessionId) {
    return getSessionTopicPrefix(sessionId) + "/rx";
  }
  
  static String getTxTopicPrefix(String sessionId) {
    return getSessionTopicPrefix(sessionId) + "/tx";
  }
}
