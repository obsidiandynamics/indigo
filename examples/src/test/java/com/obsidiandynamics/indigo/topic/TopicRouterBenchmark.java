package com.obsidiandynamics.indigo.topic;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.topic.TopicGen.*;

public final class TopicRouterBenchmark implements TestSupport {
  abstract static class Config implements Spec {
    ExecutorChoice executorChoice = null;
    long n;
    int threads;
    int bias;
    TopicGen topicGen;
    boolean assertTopicOnDelivery;
    float warmupFrac;
    LogConfig log;
    
    /* Derived fields. */
    long warmup;
    
    @Override
    public void init() {
      warmup = (long) (n * warmupFrac);
    }

    @Override
    public LogConfig getLog() {
      return log;
    }

    @Override
    public String describe() {
      return String.format("%d threads, %,d messages, %.0f%% warmup fraction", 
                           threads, n, warmupFrac * 100);
    }

    @Override
    public Summary run() throws Exception {
      return new TopicRouterBenchmark().test(this);
    }
  }
  
  static TopicGen tiny() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(1))
        .build();
  }
  
  static TopicGen small() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen medium() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen large() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen jumbo() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen mriya() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  private static class BenchSubscriber implements Subscriber {
    final Config c;
    final Topic topic;
    long received;

    BenchSubscriber(Config c, Topic topic) {
      this.c = c;
      this.topic = topic;
    }

    @Override
    public void accept(Delivery delivery) {
      if (c.assertTopicOnDelivery) assertTrue(topic.accepts(delivery.getTopic()));
      received++;
    }
  }
  
  private static class BenchTopicWatcher implements TopicWatcher {
    private final AtomicInteger created = new AtomicInteger();
    private final AtomicInteger deleted = new AtomicInteger();
    private final AtomicInteger subscribed = new AtomicInteger();
    private final AtomicInteger unsubscribed = new AtomicInteger();
    
    @Override public void created(Activation a, Topic topic) {
      created.incrementAndGet();
    }

    @Override public void deleted(Activation a, Topic topic) {
      deleted.incrementAndGet();
    }

    @Override public void subscribed(Activation a, Topic topic, Subscriber subscriber) {
      subscribed.incrementAndGet();
    }

    @Override public void unsubscribed(Activation a, Topic topic, Subscriber subscriber) {
      unsubscribed.incrementAndGet();
    }
  }
  
  @Test
  public void test() throws Exception {
    new Config() {{
      n = 100;
      threads = Runtime.getRuntime().availableProcessors();
      bias = 10;
      topicGen = medium();
      assertTopicOnDelivery = true;
      warmupFrac = .05f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
    }}.test();
  }
  
  private Summary test(Config c) throws Exception {
    final BenchTopicWatcher watcher = new BenchTopicWatcher();
    final ActorSystem system = new ActorSystemConfig() {{
      parallelism = c.threads;
      defaultActorConfig = new ActorConfig() {{ 
        bias = c.bias; 
      }};
    }}
    .createActorSystem()
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig() {{
      topicWatcher = watcher;
    }}));
    
    final ActorRef routerRef = ActorRef.of(TopicActor.ROLE);
    final List<BenchSubscriber> subscribers = new ArrayList<>();

    final List<Topic> leafTopics = c.topicGen.getLeafTopics();
    c.getLog().out.format("leaf topics: %,d\n", leafTopics.size());
    c.getLog().out.format("interests: (-): %,d, (+): %,d, (#): %,d\n", 
                          c.topicGen.getExactInterests().size(),
                          c.topicGen.getSingleLevelWildcardInterests().size(),
                          c.topicGen.getMultiLevelWildcardInterests().size());
    
    final List<Interest> interests = c.topicGen.getAllInterests();
    for (Interest interest : interests) {
      for (int i = 0; i < interest.count; i++) {
        final BenchSubscriber subscriber = new BenchSubscriber(c, interest.topic);
        subscribers.add(subscriber);
        subscribe(system, routerRef, interest.topic, subscriber).get();
      }
    }
    
    assertEquals(subscribers.size(), watcher.subscribed.get());

    final long took = TestSupport.tookThrowing(() -> {
      for (int i = 0; i < c.n; i++) {
        for (Topic leafTopic : leafTopics) {
          publish(system, routerRef, leafTopic);
        }
      }
      system.drain(0);
    });
    
    final long totalReceived = subscribers.stream().collect(Collectors.summingLong(s -> s.received)).longValue();
    assertEquals(c.n * leafTopics.size(), totalReceived);
    
    for (BenchSubscriber subscriber : subscribers) {
      unsubscribe(system, routerRef, subscriber.topic, subscriber).get();
    }
    assertEquals(subscribers.size(), watcher.unsubscribed.get());
    
    system.shutdown();
    
    final Summary summary = new Summary();
    summary.timedOps = c.n * leafTopics.size();
    summary.avgTime = took;
    return summary;
  }
  
  private static void publish(ActorSystem system, ActorRef routerRef, Topic topic) {
    system.tell(routerRef, new Publish(topic, null));
  }
  
  private static CompletableFuture<SubscribeResponse> subscribe(ActorSystem system, ActorRef routerRef, Topic topic, Subscriber subscriber) {
    return system.ask(routerRef, new Subscribe(topic, subscriber));
  }
  
  private static CompletableFuture<UnsubscribeResponse> unsubscribe(ActorSystem system, ActorRef routerRef, Topic topic, Subscriber subscriber) {
    return system.ask(routerRef, new Unsubscribe(topic, subscriber));
  }
}
