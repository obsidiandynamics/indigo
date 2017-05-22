package com.obsidiandynamics.indigo.topic;

import java.util.*;

import com.obsidiandynamics.indigo.*;

final class TopicActorState {
  final Topic topic;
  
  final Map<String, ActorRef> subtopics = new HashMap<>();
  
  final Map<Topic, Set<Subscriber>> subscribers = new HashMap<>();

  TopicActorState(Topic topic) {
    this.topic = topic;
  }
  
  void subscribe(Topic topic, Subscriber subscriber) {
    final Set<Subscriber> subscriberSet;
    final Set<Subscriber> existingSet = subscribers.get(topic);
    if (existingSet != null) {
      subscriberSet = existingSet;
    } else {
      subscribers.put(topic, subscriberSet = new HashSet<>());
    }
    subscriberSet.add(subscriber);
  }
  
  boolean unsubscribe(Topic topic, Subscriber subscriber) {
    final Set<Subscriber> subscriberSet = subscribers.get(topic);
    if (subscriberSet != null) {
      subscriberSet.remove(subscriber);
      if (subscriberSet.isEmpty()) {
        subscribers.remove(topic);
      }
    }
    return subscribers.isEmpty();
  }

  @Override
  public String toString() {
    return "TopicActorState [topic=" + topic + ", subtopics=" + subtopics.keySet() + ", subscribers=" + subscribers + "]";
  }
}
