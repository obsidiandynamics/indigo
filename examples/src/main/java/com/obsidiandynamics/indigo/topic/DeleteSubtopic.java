package com.obsidiandynamics.indigo.topic;

final class DeleteSubtopic {
  private static final DeleteSubtopic INSTANCE = new DeleteSubtopic();
  
  static DeleteSubtopic instance() { return INSTANCE; }
  
  private DeleteSubtopic() {}

  @Override
  public String toString() {
    return "DeleteSubtopic";
  }
}
