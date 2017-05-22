package com.obsidiandynamics.indigo.topic;

import java.util.*;

import com.obsidiandynamics.indigo.*;

final class TopicActorState {
  final Topic topic;
  
  final Map<String, ActorRef> subtopics = new HashMap<>();
  
  final Set<Subscriber> subscribers = new HashSet<>();

  TopicActorState(Topic topic) {
    this.topic = topic;
  }

  @Override
  public String toString() {
    return "TopicActorState [topic=" + topic + ", subtopics=" + subtopics.keySet() + ", subscribers=" + subscribers + "]";
  }
}
