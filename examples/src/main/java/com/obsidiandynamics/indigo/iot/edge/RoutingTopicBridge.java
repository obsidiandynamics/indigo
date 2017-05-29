package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;

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
    system.on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig()));
  }

  @Override
  public void onConnect(EdgeNexus nexus) {
    if (LOG.isDebugEnabled()) LOG.debug("Connected to {}", nexus);
  }

  @Override
  public void onDisconnect(EdgeNexus nexus) {
    if (LOG.isDebugEnabled()) LOG.debug("Disconnected from {}", nexus);
    //TODO clean up subscriptions
  }

  @Override
  public CompletableFuture<SubscribeResponseFrame> onSubscribe(EdgeNexus nexus, SubscribeFrame sub) {
    final Subscriber subscriber = d -> {
      if (LOG.isTraceEnabled()) LOG.trace("Delivering {} to {}", d.getPayload(), nexus);
      nexus.sendAuto(d.getPayload());
    };
    final CompletableFuture<SubscribeResponseFrame> future = new CompletableFuture<>();
    system.ingress(a -> {
      final List<SubscribeResponse> responses = new ArrayList<>(sub.getTopics().length);
      for (String topic : sub.getTopics()) {
        a.to(routerRef).ask(new Subscribe(Topic.of(topic), subscriber))
        .onFault(f -> {
          LOG.warn("Fault while handling subscription to topic {}: {}", topic, f);
          future.completeExceptionally(new FaultException(f.getReason()));
        })
        .onResponse(r -> {
          responses.add(r.body());
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
    system.tell(routerRef, new Publish(Topic.of(pub.getTopic()), new TextFrame(pub.getPayload())));
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    if (LOG.isTraceEnabled()) LOG.trace("{} published {}", nexus, pub);
    system.tell(routerRef, new Publish(Topic.of(pub.getTopic()), new BinaryFrame(pub.getPayload())));
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
