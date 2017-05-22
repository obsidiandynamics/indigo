package com.obsidiandynamics.indigo.topic;

public final class PassivateSubtopic {
  private static final PassivateSubtopic INSTANCE = new PassivateSubtopic();
  
  static PassivateSubtopic instance() { return INSTANCE; }
  
  PassivateSubtopic() {}

  @Override
  public String toString() {
    return "PassivateSubtopic";
  }
}
