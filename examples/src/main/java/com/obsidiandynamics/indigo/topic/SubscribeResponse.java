package com.obsidiandynamics.indigo.topic;

public final class SubscribeResponse {
  private static final SubscribeResponse INSTANCE = new SubscribeResponse();
  
  static SubscribeResponse instance() { return INSTANCE; }
  
  private SubscribeResponse() {}

  @Override
  public String toString() {
    return "SubscribeResponse";
  }
}
