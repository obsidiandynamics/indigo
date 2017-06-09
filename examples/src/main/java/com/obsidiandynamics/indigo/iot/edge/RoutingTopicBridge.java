package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.topic.*;

public final class RoutingTopicBridge implements TopicBridge {
  private static final Logger LOG = LoggerFactory.getLogger(RoutingTopicBridge.class);
  
  private final ActorSystem system;
  
  private final ActorRef routerRef = ActorRef.of(TopicRouter.ROLE);
  
  public RoutingTopicBridge() {
    this(ActorSystem.create());
  }
  
  private RoutingTopicBridge(ActorSystem system) {
    this.system = system;
    system.on(TopicRouter.ROLE).withConfig(new ActorConfig() {{
      bias = 10;
    }}).cue(() -> new TopicRouter(new TopicConfig()));
  }

  @Override
  public void onConnect(EdgeNexus nexus) {
    if (LOG.isDebugEnabled()) LOG.debug("{}: connected", nexus);
    final Subscriber subscriber = d -> {
      if (LOG.isTraceEnabled()) LOG.trace("{}: delivering {}", nexus, d.getPayload());
      nexus.sendAuto(d.getPayload());
    };
    nexus.getSession().setSubscription(new RoutingSubscription(subscriber));
  }

  @Override
  public void onDisconnect(EdgeNexus nexus) {
    if (LOG.isDebugEnabled()) LOG.debug("{}: disconnected", nexus);
    final RoutingSubscription subscription = nexus.getSession().getSubscription();
    if (subscription == null) {
      LOG.error("{}: no subscription", nexus);
      return;
    }
    for (Topic topic : subscription.getSubscribedTopics()) {
      if (LOG.isTraceEnabled()) LOG.trace("{}: unsubscribing from {}", nexus, topic);
      system.tell(routerRef, new Unsubscribe(topic, subscription.getSubscriber()));
    }
  }

  @Override
  public CompletableFuture<Void> onBind(EdgeNexus nexus, Set<String> subscribe) {
    if (subscribe.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    
    final RoutingSubscription subscription = nexus.getSession().getSubscription();
    if (subscription == null) {
      LOG.error("{}: no subscription", nexus);
      throw new IllegalStateException("No subscription set for " + nexus);
    }

    final CompletableFuture<Void> future = new CompletableFuture<>();
    final List<Topic> topics = subscribe.stream().map(t -> Topic.of(t)).collect(Collectors.toList());
    
    system.ingress(a -> {
      final List<SubscribeResponse> responses = new ArrayList<>(topics.size());
      for (final Topic topic : topics) {
        a.to(routerRef).ask(new Subscribe(topic, subscription.getSubscriber()))
        .onFault(f -> {
          LOG.warn("{}: fault while handling subscription to topic {}: {}", nexus, topic, f);
          future.completeExceptionally(new FaultException(f.getReason()));
        })
        .onResponse(r -> {
          responses.add(r.body());
          subscription.addTopic(topic);
          if (responses.size() == topics.size()) {
            future.complete(null);
          }
        });
      } 
    });
    return future;
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
    if (LOG.isTraceEnabled()) LOG.trace("{}: published {}", nexus, pub);
    system.tell(routerRef, new Publish(Topic.of(pub.getTopic()), new TextFrame(pub.getTopic(), pub.getPayload())));
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    if (LOG.isTraceEnabled()) LOG.trace("{}: published {}", nexus, pub);
    system.tell(routerRef, new Publish(Topic.of(pub.getTopic()), new BinaryFrame(pub.getTopic(), pub.getPayload())));
  }

  @Override
  public void close() throws Exception {
    if (LOG.isDebugEnabled()) LOG.trace("Closing bridge");
    system.shutdown();
    final List<Fault> dlq = system.getDeadLetterQueue();
    if (! dlq.isEmpty()) {
      LOG.warn("Actor system had a non-empty dead-letter queue");
      for (Fault fault : dlq) {
        LOG.warn("> {}", fault);
      }
    }
  }
}
