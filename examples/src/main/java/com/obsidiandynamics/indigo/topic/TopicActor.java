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
    final Topic current = Topic.fromRef(a.self());
    state = new TopicActorState(current);
    if (! current.isRoot()) {
      config.topicWatcher.created(a, current);
      final Topic parent = current.parent();
      if (parent != null) {
        a.to(parent.asRef()).ask(CreateSubtopic.instance()).onFault(a::propagateFault).onResponse(r -> {});
      }
    }
  }
  
  @Override
  public void passivated(Activation a) {
    if (LOG.isTraceEnabled()) LOG.trace("{} passivating", a.self());
    final Topic current = Topic.fromRef(a.self());
    if (! current.isRoot()) {
      config.topicWatcher.deleted(a, current);
      final Topic parent = current.parent();
      if (parent != null) {
        a.to(parent.asRef()).ask(DeleteSubtopic.instance()).onFault(a::propagateFault).onResponse(r -> {});
      }
    }
  }
  
  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Subscribe.class).then(b -> subscribe(a, m))
    .when(Unsubscribe.class).then(b -> unsubscribe(a, m))
    .when(Publish.class).then(b -> publish(a, m))
    .when(CreateSubtopic.class).then(b -> createSubtopic(a, m))
    .when(DeleteSubtopic.class).then(b -> deleteSubtopic(a, m))
    .otherwise(a::messageFault);
  }
  
  /**
   *  Called by the child of this actor during its activation, thereby registering itself
   *  with its parent.
   *  
   *  @param a
   *  @param m
   */
  private void createSubtopic(Activation a, Message m) {
    final Topic subtopic = Topic.fromRef(m.from());
    if (LOG.isTraceEnabled()) LOG.trace("{} creating {}", a.self(), subtopic.lastPart());
    state.subtopics.put(subtopic.lastPart(), m.from());
    a.reply(m).tell();
  }
  
  /**
   *  Called by the child of this actor during its passivation, thereby deregistering itself
   *  from its parent. If there are no subtopics left, this actor will do the same.
   *  
   *  @param a
   *  @param m
   */
  private void deleteSubtopic(Activation a, Message m) {
    final Topic subtopic = Topic.fromRef(m.from());
    if (LOG.isTraceEnabled()) LOG.trace("{} deleting {}", a.self(), subtopic.lastPart());
    state.subtopics.remove(subtopic.lastPart());
    if (state.subtopics.isEmpty() && state.subscribers.isEmpty()) {
      a.passivate();
    }
    a.reply(m).tell();
  }
  
  private void subscribe(Activation a, Message m) {
    final Subscribe subscribe = m.body();
    if (LOG.isTraceEnabled()) LOG.trace("{} processing subscribe to {}", a.self(), subscribe.getTopic());
    final ActorRef subtopicRef = resolveDeepTopicRef(subscribe.getTopic());
    if (subtopicRef != null) {
      // the request is for a subtopic - delegate down
      if (LOG.isTraceEnabled()) LOG.trace("{} delegating to {}", a.self(), subtopicRef);
      a.forward(m).to(subtopicRef);
    } else {
      if (LOG.isTraceEnabled()) LOG.trace("{} adding to {}", a.self(), subscribe.getTopic());
      // the request is for the current level or a '+' wildcard - subscribe and reply
      final boolean added = state.subscribe(subscribe.getTopic(), subscribe.getSubscriber());
      if (added) config.topicWatcher.subscribed(a, subscribe.getTopic(), subscribe.getSubscriber());
      
      a.reply(m).tell(SubscribeResponse.instance());
    }
  }
  
  private void unsubscribe(Activation a, Message m) {
    final Unsubscribe unsubscribe = m.body();
    if (LOG.isTraceEnabled()) LOG.trace("{} processing unsubscribe from {}", a.self(), unsubscribe.getTopic());
    final ActorRef subtopicRef = resolveDeepTopicRef(unsubscribe.getTopic());
    if (subtopicRef != null) {
      // the request is for a subtopic - delegate down
      if (LOG.isTraceEnabled()) LOG.trace("{} delegating to {}", a.self(), subtopicRef);
      a.forward(m).to(subtopicRef);
    } else {
      if (LOG.isTraceEnabled()) LOG.trace("{} removing from {}", a.self(), unsubscribe.getTopic());
      // the request is for the current level or a '+' wildcard - subscribe and reply
      final boolean removed = state.unsubscribe(unsubscribe.getTopic(), unsubscribe.getSubscriber());
      if (removed) config.topicWatcher.unsubscribed(a, unsubscribe.getTopic(), unsubscribe.getSubscriber());
      if (state.subtopics.isEmpty() && state.subscribers.isEmpty()) {
        a.passivate();
      }
      
      a.reply(m).tell(UnsubscribeResponse.instance());
    }
  }
  
  private ActorRef resolveDeepTopicRef(Topic topic) {
    if (isDeeper(topic) && ! nextTopicPart(topic).equals(Topic.SL_WILDCARD)) {
      // the request is for a subtopic - delegate down
      final String next = nextTopicPart(topic);
      final ActorRef subtopicRef;
      final ActorRef existingNextRef = state.subtopics.get(next);
      if (existingNextRef != null) {
        subtopicRef = existingNextRef;
      } else {
        final Topic subtopic = state.topic.append(next);
        subtopicRef = subtopic.asRef();
      }
      return subtopicRef;
    } else {
      return null;
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
          subscriber.accept(delivery);
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
