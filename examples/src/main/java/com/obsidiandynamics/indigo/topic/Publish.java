package com.obsidiandynamics.indigo.topic;

public final class Publish {
  private final Topic topic;
  
  private final Object payload;

  public Publish(Topic topic, Object payload) {
    this.topic = topic;
    this.payload = payload;
  }
  
  public Topic getTopic() {
    return topic;
  }

  public Object getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return "Publish [topic=" + topic + ", payload=" + payload + "]";
  }
}
