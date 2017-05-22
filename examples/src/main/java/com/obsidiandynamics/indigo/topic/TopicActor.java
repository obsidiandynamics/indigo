package com.obsidiandynamics.indigo.topic;

import java.util.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.*;

public final class TopicActor implements Actor {
  private static final Logger LOG = LoggerFactory.getLogger(TopicActor.class);
  
  public static final String ROLE = "topic_router";
  
  private final TopicConfig config;
  
  private TopicActorState state;

  public TopicActor(TopicConfig config) {
    this.config = config;
  }
  
  @Override
  public void activated(Activation a) {
    if (LOG.isTraceEnabled()) LOG.trace("{} activating", a.self());
    final String selfKey = a.self().key();
    final Topic topic = selfKey != null ? Topic.of(selfKey) : Topic.root();
    state = new TopicActorState(selfKey != null ? Topic.of(selfKey) : Topic.root());
    if (selfKey != null) {
      final Topic parent = topic.parent();
      final String parentKey = parent.isRoot() ? null : parent.toString();
      a.to(ActorRef.of(ROLE, parentKey)).ask(ActivateSubtopic.instance()).onFault(a::propagateFault).onResponse(r -> {});
    }
  }
  
  @Override
  public void passivated(Activation a) {
    if (LOG.isTraceEnabled()) LOG.trace("{} passivating", a.self());
  }

  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Subscribe.class).then(b -> subscribe(a, m))
    .when(Publish.class).then(b -> publish(a, m))
    .when(ActivateSubtopic.class).then(b -> activateSubtopic(a, m))
    .otherwise(a::messageFault);
  }
  
  /**
   *  Called by the child of this actor during its activation, thereby registering itself
   *  with its parent.
   *  
   *  @param a
   *  @param m
   */
  private void activateSubtopic(Activation a, Message m) {
    final Topic subtopic = Topic.of(m.from().key());
    final ActorRef subtopicRef = ActorRef.of(ROLE, subtopic.toString());
    if (LOG.isTraceEnabled()) LOG.trace("{} registering {}", a.self(), subtopic.lastPart());
    state.subtopics.put(subtopic.lastPart(), subtopicRef);
    a.reply(m).tell();
  }
  
  private void subscribe(Activation a, Message m) {
    final Subscribe subscribe = m.body();
    if (LOG.isTraceEnabled()) LOG.trace("{} processing subscribe to {}", a.self(), subscribe.getTopic());
    if (isDeeper(subscribe.getTopic()) && ! nextTopicPart(subscribe.getTopic()).equals(Topic.SL_WILDCARD)) {
      // the request is for a subtopic - delegate down
      final String next = nextTopicPart(subscribe.getTopic());
      final ActorRef subtopicRef;
      final ActorRef existingNextRef = state.subtopics.get(next);
      if (existingNextRef != null) {
        subtopicRef = existingNextRef;
      } else {
        final Topic subtopic = state.topic.append(next);
        subtopicRef = ActorRef.of(ROLE, subtopic.toString());
      }
      if (LOG.isTraceEnabled()) LOG.trace("{} delegating to {}", a.self(), subtopicRef);
      a.forward(m).to(subtopicRef);
    } else {
      if (LOG.isTraceEnabled()) LOG.trace("{} adding to {}", a.self(), subscribe.getTopic());
      // the request is for the current level or a '+' wildcard - subscribe and reply
      state.subscribe(subscribe.getTopic(), subscribe.getSubscriber());
      
      a.reply(m).tell(SubscribeResponse.instance());
    }
  }
  
  private void publish(Activation a, Message m) {
    final Publish publish = m.body();
    if (LOG.isTraceEnabled()) LOG.trace("{} processing subscribe to {}", a.self(), publish.getTopic());
    final Delivery delivery = new Delivery(publish.getTopic(), publish.getPayload());
    for (Map.Entry<Topic, Set<Subscriber>> entry : state.subscribers.entrySet()) {
      if (entry.getKey().accepts(publish.getTopic())) {
        if (LOG.isTraceEnabled()) LOG.trace("{} matched {}", a.self(), entry.getKey());
        // subscribers match the exact topic in the publish command
        for (Subscriber subscriber : entry.getValue()) {
          a.egress(subscriber::accept).withExecutor(config.executorName).tell(delivery);
        }
      } else {
        if (LOG.isTraceEnabled()) LOG.trace("{} skipped {}", a.self(), entry.getKey());
      }
    }

    final ActorRef subtopicRef;
    if (isDeeper(publish.getTopic())) {
      // the request is for a subtopic - delegate down if one has been created
      final String next = nextTopicPart(publish.getTopic());
      subtopicRef = state.subtopics.get(next);
      if (subtopicRef != null) {
        if (LOG.isTraceEnabled()) LOG.trace("{} delegating to {}", a.self(), subtopicRef);
        a.forward(m).to(subtopicRef);
      } else {
        if (LOG.isTraceEnabled()) LOG.trace("{} no subtopics", a.self());
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
  
//  private boolean isRoot() {
//    return state.topic.isRoot();
//  }
//  
//  private String currentPart(Topic topic) {
//    return topic.getParts()[state.topic.length() - 1];
//  }
  
  private String nextTopicPart(Topic topic) {
    return topic.getParts()[state.topic.length()];
  }
  
  private boolean isDeeper(Topic topic) {
    final int thisLength = state.topic.length();
    final int thatLength = topic.length();
    if (thatLength == thisLength + 1) {
      return ! topic.isMultiLevelWildcard();
    } else {
      return thatLength > thisLength;
    }
  }
}
