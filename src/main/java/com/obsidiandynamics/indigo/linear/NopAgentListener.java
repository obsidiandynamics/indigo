package com.obsidiandynamics.indigo.linear;

public final class NopAgentListener implements AgentListener {
  static final NopAgentListener INSTANCE = new NopAgentListener();
  
  static NopAgentListener getInstance() { return INSTANCE; }
  
  @Override
  public void agentActivated(String key) {}

  @Override
  public void agentPassivated(String key) {}
}
