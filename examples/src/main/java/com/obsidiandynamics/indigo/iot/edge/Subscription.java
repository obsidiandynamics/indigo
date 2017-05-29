package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.topic.*;

final class Subscription {
  private final Subscriber subscriber;
  private final List<Topic> topics = new CopyOnWriteArrayList<>();
  
  public Subscription(Subscriber subscriber) {
    this.subscriber = subscriber;
  }
  
  Subscriber getSubscriber() {
    return subscriber;
  }
  
  void addTopic(Topic topic) {
    topics.add(topic);
  }
  
  void addTopics(List<Topic> toAdd) {
    topics.addAll(toAdd);
  }
  
  void removeTopics(List<Topic> toRemove) {
    topics.removeAll(toRemove);
  }
  
  List<Topic> getTopics() {
    return Collections.unmodifiableList(topics);
  }
}
