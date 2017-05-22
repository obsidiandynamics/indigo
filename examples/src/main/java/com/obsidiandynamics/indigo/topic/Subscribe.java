package com.obsidiandynamics.indigo.topic;

public final class Subscribe {
  private final Topic topic;
  
  private final Subscriber subscriber;

  public Subscribe(Topic topic, Subscriber subscriber) {
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
    return "Subscribe [topic=" + topic + "]";
  }
}
