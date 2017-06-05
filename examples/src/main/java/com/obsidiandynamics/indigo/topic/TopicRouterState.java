package com.obsidiandynamics.indigo.topic;

import java.util.*;

import com.obsidiandynamics.indigo.*;

final class TopicRouterState {
  final Topic topic;
  
  final Map<String, ActorRef> subtopics = new HashMap<>();
  
  final Map<Topic, Set<Subscriber>> subscribers = new HashMap<>();

  TopicRouterState(Topic topic) {
    this.topic = topic;
  }
  
  boolean subscribe(Topic topic, Subscriber subscriber) {
    final Set<Subscriber> subscriberSet;
    final Set<Subscriber> existingSet = subscribers.get(topic);
    if (existingSet != null) {
      subscriberSet = existingSet;
    } else {
      subscribers.put(topic, subscriberSet = new HashSet<>());
    }
    return subscriberSet.add(subscriber);
  }
  
  boolean unsubscribe(Topic topic, Subscriber subscriber) {
    final Set<Subscriber> subscriberSet = subscribers.get(topic);
    if (subscriberSet != null) {
      final boolean removed = subscriberSet.remove(subscriber);
      if (subscriberSet.isEmpty()) {
        subscribers.remove(topic);
      }
      return removed;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "TopicRouterState [topic=" + topic + ", subtopics=" + subtopics.keySet() + ", subscribers=" + subscribers + "]";
  }
}
