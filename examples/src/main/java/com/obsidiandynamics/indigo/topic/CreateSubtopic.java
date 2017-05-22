package com.obsidiandynamics.indigo.topic;

final class CreateSubtopic {
  private static final CreateSubtopic INSTANCE = new CreateSubtopic();
  
  static CreateSubtopic instance() { return INSTANCE; }
  
  private CreateSubtopic() {}

  @Override
  public String toString() {
    return "CreateSubtopic";
  }
}
