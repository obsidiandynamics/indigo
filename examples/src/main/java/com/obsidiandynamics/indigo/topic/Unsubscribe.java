package com.obsidiandynamics.indigo.topic;

public final class Unsubscribe {
  private final Topic topic;
  
  private final Subscriber subscriber;

  public Unsubscribe(Topic topic, Subscriber subscriber) {
    this.topic = topic;
    this.subscriber = subscriber;
  }

  Topic getTopic() {
    return topic;
  }
  
  Subscriber getSubscriber() {
    return subscriber;
  }

  @Override
  public String toString() {
    return "Unsubscribe [topic=" + topic + "]";
  }
}
