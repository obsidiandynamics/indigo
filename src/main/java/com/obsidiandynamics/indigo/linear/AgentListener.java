package com.obsidiandynamics.indigo.linear;

public interface AgentListener {
  void agentActivated(String key);
  
  void agentPassivated(String key);
}
