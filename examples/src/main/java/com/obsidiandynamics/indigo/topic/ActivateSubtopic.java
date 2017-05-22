package com.obsidiandynamics.indigo.topic;

public final class ActivateSubtopic {
  private static final ActivateSubtopic INSTANCE = new ActivateSubtopic();
  
  static ActivateSubtopic instance() { return INSTANCE; }
  
  ActivateSubtopic() {}

  @Override
  public String toString() {
    return "ActivateSubtopic";
  }
}
