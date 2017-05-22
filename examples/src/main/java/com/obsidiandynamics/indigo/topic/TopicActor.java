package com.obsidiandynamics.indigo.topic;

import com.obsidiandynamics.indigo.*;

public final class TopicActor implements Actor {
  public static final String ROLE = "topic_router";
  
  private final TopicConfig config;
  
  private TopicActorState state;

  public TopicActor(TopicConfig config) {
    this.config = config;
  }
  
  @Override
  public void activated(Activation a) {
    final String selfKey = a.self().key();
    state = new TopicActorState(selfKey != null ? Topic.of(selfKey) : Topic.root());
    System.out.println("activated " + state.topic);
  }
  
  @Override
  public void passivated(Activation a) {
    
  }

  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Subscribe.class).then(b -> subscribe(a, m))
    .when(Publish.class).then(b -> publish(a, m))
    .otherwise(a::messageFault);
  }
  
  private void subscribe(Activation a, Message m) {
    final Subscribe subscribe = m.body();
    System.out.println(a.self() + " got " + subscribe);
    if (isDeeper(subscribe.getTopic())) {
      // the request is for a subtopic - delegate down
      final String next = nextTopicPart(subscribe.getTopic());
      final ActorRef subtopicRef;
      final ActorRef existingNextRef = state.subtopics.get(next);
      if (existingNextRef != null) {
        subtopicRef = existingNextRef;
      } else {
        final Topic subtopic = state.topic.append(next);
        subtopicRef = ActorRef.of(ROLE, subtopic.toString());
        state.subtopics.put(next, subtopicRef);
      }
      a.forward(m).to(subtopicRef);
    } else {
      // the request is for the current level - subscribe and reply
      state.subscribers.add(subscribe.getSubscriber());
      
      a.reply(m).tell(SubscribeResponse.instance());
    }
  }
  
  private void publish(Activation a, Message m) {
    final Publish publish = m.body();
    System.out.println(a.self() + " got " + publish);
    final Delivery delivery = new Delivery(publish.getTopic(), publish.getPayload());
    for (Subscriber subscriber : state.subscribers) {
      a.egress(subscriber::accept).withExecutor(config.executorName).tell(delivery);
    }

    final ActorRef subtopicRef;
    if (isDeeper(publish.getTopic())) {
      // the request is for a subtopic - delegate down if one has been created
      final String next = nextTopicPart(publish.getTopic());
      subtopicRef = state.subtopics.get(next);
      if (subtopicRef != null) {
        a.forward(m).to(subtopicRef);
      }
    } else {
      // the request is for the current level - no further delegation
      subtopicRef = null;
    }
    
    if (subtopicRef == null) {
      // no delegation took place - reply in case the publisher cares
      a.reply(m).tell(PublishResponse.instance());
    }
  }
  
  private String nextTopicPart(Topic topic) {
    return topic.getParts()[state.topic.length()];
  }
  
  private boolean isDeeper(Topic topic) {
    return topic.length() > state.topic.length();
  }
}
