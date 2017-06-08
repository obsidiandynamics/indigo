package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import com.obsidiandynamics.indigo.topic.*;

final class RoutingSubscription implements Subscription {
  private final Subscriber subscriber;
  private final List<Topic> topics = new CopyOnWriteArrayList<>();
  
  RoutingSubscription(Subscriber subscriber) {
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
  
  List<Topic> getSubscribedTopics() {
    return Collections.unmodifiableList(topics);
  }

  @Override
  public Collection<String> getTopics() {
    return topics.stream().map(t -> t.toString()).collect(Collectors.toList());
  }
}
