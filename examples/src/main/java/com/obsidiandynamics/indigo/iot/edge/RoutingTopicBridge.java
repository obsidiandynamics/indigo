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
  
  private final ActorRef routerRef = ActorRef.of(TopicActor.ROLE);
  
  public RoutingTopicBridge() {
    this(ActorSystem.create());
  }
  
  public RoutingTopicBridge(ActorSystem system) {
    this.system = system;
    system.on(TopicActor.ROLE).withConfig(new ActorConfig() {{
      bias = 10;
    }}).cue(() -> new TopicActor(new TopicConfig()));
  }

  @Override
  public void onConnect(EdgeNexus nexus) {
    if (LOG.isDebugEnabled()) LOG.debug("Connected to {}", nexus);
    final Subscriber subscriber = d -> {
      if (LOG.isTraceEnabled()) LOG.trace("Delivering {} to {}", d.getPayload(), nexus);
      nexus.sendAuto(d.getPayload());
    };
    nexus.setContext(new Subscription(subscriber));
  }

  @Override
  public void onDisconnect(EdgeNexus nexus) {
    if (LOG.isDebugEnabled()) LOG.debug("Disconnected from {}", nexus);
    final Subscription subscription = nexus.getContext();
    if (subscription == null) {
      LOG.error("No subscription set for {}", nexus);
      return;
    }
    for (Topic topic : subscription.getTopics()) {
      if (LOG.isTraceEnabled()) LOG.trace("Unsubscribing {} from {}", nexus, topic);
      system.tell(routerRef, new Unsubscribe(topic, subscription.getSubscriber()));
    }
  }

  @Override
  public CompletableFuture<SubscribeResponseFrame> onSubscribe(EdgeNexus nexus, SubscribeFrame sub) {
    final Subscription subscription = nexus.getContext();
    if (subscription == null) {
      LOG.error("No subscription set for {}", nexus);
      throw new IllegalStateException("No subscription set for " + nexus);
    }
    
    final List<Topic> topics = Arrays.stream(sub.getTopics(), 0, sub.getTopics().length)
        .map(t -> Topic.of(t)).collect(Collectors.toList());
    
    final CompletableFuture<SubscribeResponseFrame> future = new CompletableFuture<>();
    system.ingress(a -> {
      final List<SubscribeResponse> responses = new ArrayList<>(sub.getTopics().length);
      for (final Topic topic : topics) {
        a.to(routerRef).ask(new Subscribe(topic, subscription.getSubscriber()))
        .onFault(f -> {
          LOG.warn("Fault while handling subscription to topic {}: {}", topic, f);
          future.completeExceptionally(new FaultException(f.getReason()));
        })
        .onResponse(r -> {
          responses.add(r.body());
          subscription.addTopic(topic);
          if (responses.size() == sub.getTopics().length) {
            future.complete(new SubscribeResponseFrame(sub.getId(), null));
          }
        });
      } 
    });
    return future;
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
    if (LOG.isTraceEnabled()) LOG.trace("{} published {}", nexus, pub);
    system.tell(routerRef, new Publish(Topic.of(pub.getTopic()), new TextFrame(pub.getTopic(), pub.getPayload())));
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    if (LOG.isTraceEnabled()) LOG.trace("{} published {}", nexus, pub);
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
