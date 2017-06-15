package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import com.obsidiandynamics.indigo.topic.*;

final class RoutingSubscription implements Subscription {
  private final Subscriber subscriber;
  private final Set<Topic> topics = new CopyOnWriteArraySet<>();
  
  RoutingSubscription(Subscriber subscriber) {
    this.subscriber = subscriber;
  }
  
  Subscriber getSubscriber() {
    return subscriber;
  }
  
  void addTopic(Topic topic) {
    topics.add(topic);
  }

  void removeTopic(Topic topic) { topics.remove(topic); }
  
  void addTopics(List<Topic> toAdd) {
    topics.addAll(toAdd);
  }
  
  void removeTopics(List<Topic> toRemove) {
    topics.removeAll(toRemove);
  }
  
  Set<Topic> getSubscribedTopics() {
    return Collections.unmodifiableSet(topics);
  }

  @Override
  public Set<String> getTopics() {
    return topics.stream().map(t -> t.toString()).collect(Collectors.toSet());
  }
}
