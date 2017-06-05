package com.obsidiandynamics.indigo.topic;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.topic.TopicSpec.*;

public final class TopicRouterBenchmark implements TestSupport {
  abstract static class Config implements Spec {
    ExecutorChoice executorChoice = null;
    long n;
    int threads;
    int bias;
    TopicSpec topicSpec;
    boolean assertTopicOnDelivery;
    float warmupFrac;
    LogConfig log;
    
    /* Derived fields. */
    long warmup;
    
    List<Topic> leafTopics;
    List<Interest> exactInterests;
    List<Interest> singleLevelWildcardInterests;
    List<Interest> multiLevelWildcardInterests;
    List<Interest> interests;
    List<BenchSubscriber> subscribers;
    List<Topic> targetTopics;
    int amplification;
    
    private boolean initialised;
    
    @Override
    public void init() {
      if (initialised) return;
      
      warmup = (long) (n * warmupFrac);
      
      leafTopics = topicSpec.getLeafTopics();
      exactInterests = topicSpec.getExactInterests();
      singleLevelWildcardInterests = topicSpec.getSingleLevelWildcardInterests();
      multiLevelWildcardInterests = topicSpec.getMultiLevelWildcardInterests();
      interests = new ArrayList<>(exactInterests.size() + singleLevelWildcardInterests.size() + multiLevelWildcardInterests.size());
      interests.addAll(exactInterests);
      interests.addAll(singleLevelWildcardInterests);
      interests.addAll(multiLevelWildcardInterests);
      
      subscribers = new ArrayList<>();
      for (Interest interest : interests) {
        for (int i = 0; i < interest.count; i++) {
          subscribers.add(new BenchSubscriber(this, interest.topic));
        }
      }

      targetTopics = exactInterests.stream().map(s -> s.topic).collect(Collectors.toList());
      amplification = matchingSubscribers(targetTopics, subscribers);
      
      initialised = true;
    }
    
    private static int matchingSubscribers(List<Topic> topics, List<BenchSubscriber> subscribers) {
      int matching = 0;
      for (BenchSubscriber subscriber : subscribers) {
        for (Topic topic : topics) {
          if (subscriber.topic.accepts(topic)) {
            matching++;
          }
        }
      }
      return matching;
    }

    @Override
    public LogConfig getLog() {
      return log;
    }

    @Override
    public String describe() {
      return String.format("%d threads, %,d messages, %.0f%% warmup fraction\n" + 
                           "(~): %,d, (-): %,d, (+): %,d, (#): %,d, (><): %,d", 
                           threads,
                           n, 
                           warmupFrac * 100,
                           leafTopics.size(),
                           exactInterests.size(),
                           singleLevelWildcardInterests.size(),
                           multiLevelWildcardInterests.size(),
                           amplification);
    }

    @Override
    public Summary run() throws Exception {
      return new TopicRouterBenchmark().test(this);
    }
  }
  
  static TopicSpec tiny() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(1))
        .build();
  }
  
  static TopicSpec small() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  static TopicSpec medium() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  static TopicSpec large() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  static TopicSpec jumbo() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .build();
  }
  
  static TopicSpec mriya() {
    return TopicSpec.builder()
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
        .add(new NodeSpec(1, 1, 1).nodes(2))
        .add(new NodeSpec(1, 1, 1).nodes(5))
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
    
    void reset() {
      received = 0;
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
    test(TopicRouterBenchmark::tiny, 1000);
    test(TopicRouterBenchmark::small, 100);
    test(TopicRouterBenchmark::medium, 10);
  }
  
  private void test(Supplier<TopicSpec> topicGenSupplier, int n) throws Exception {
    final int _n = n;
    new Config() {{
      n = _n;
      threads = Runtime.getRuntime().availableProcessors();
      bias = 10;
      topicSpec = topicGenSupplier.get();
      assertTopicOnDelivery = true;
      warmupFrac = 0.05f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
    }}.test();
  }
  
  private Summary test(Config c) throws Exception {
    final BenchTopicWatcher watcher = new BenchTopicWatcher();
    final ActorSystem system = new ActorSystemConfig() {{
      parallelism = c.threads;
      if (c.executorChoice != null) {
        executor = c.executorChoice;
      }
      defaultActorConfig = new ActorConfig() {{ 
        bias = c.bias; 
      }};
    }}
    .createActorSystem()
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig() {{
      topicWatcher = watcher;
    }}));
    
    final ActorRef routerRef = ActorRef.of(TopicActor.ROLE);
    for (BenchSubscriber subscriber : c.subscribers) {
      subscriber.reset();
      subscribe(system, routerRef, subscriber.topic, subscriber).get();
    }
    system.drain(0);
    assertEquals(c.subscribers.size(), watcher.subscribed.get());
    assertEquals(c.exactInterests.size(), watcher.created.get());

    final long o = c.n - c.warmup;
    final long warmupReceived;
    final long progressInterval = Math.max(1, o / 25);
    if (c.warmup != 0) {
      if (c.log.stages) c.log.out.format("Warming up...\n");
      for (int i = 0; i < c.warmup; i++) {
        for (Topic topic : c.targetTopics) {
          publish(system, routerRef, topic);
        }
        if (c.log.progress && i % progressInterval == 0) c.log.printProgressBlock();
      }
      system.drain(0);
      warmupReceived = c.subscribers.stream().collect(Collectors.summingLong(s -> s.received)).longValue();
    } else {
      warmupReceived = 0;
    }

    if (c.log.stages) c.log.out.format("Starting timed run...\n");
    final long took = TestSupport.tookThrowing(() -> {
      for (int i = 0; i < o; i++) {
        for (Topic topic : c.targetTopics) {
          publish(system, routerRef, topic);
        }
        if (c.log.progress && (i + c.warmup) % progressInterval == 0) c.log.printProgressBlock();
      }
      system.drain(0);
    });
    
    final long totalReceived = c.subscribers.stream().collect(Collectors.summingLong(s -> s.received)).longValue();
    assertEquals(c.n * c.amplification, totalReceived);
    
    for (BenchSubscriber subscriber : c.subscribers) {
      unsubscribe(system, routerRef, subscriber.topic, subscriber).get();
    }
    assertEquals(c.subscribers.size(), watcher.unsubscribed.get());

    system.shutdown();

    assertEquals(c.exactInterests.size(), watcher.deleted.get());
    
    final Summary summary = new Summary();
    summary.timedOps = totalReceived - warmupReceived;
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
  
  public static void main(String[] args) throws Exception {
    new Config() {{
      executorChoice = ActorSystemConfig.ExecutorChoice.AUTO;
      n = 1000;
      warmupFrac = .05f;
      threads = Runtime.getRuntime().availableProcessors();
      bias = 10;
      topicSpec = large();
      assertTopicOnDelivery = false;
      warmupFrac = .05f;
      log = new LogConfig() {{
        progress = intermediateSummaries = true;
        summary = true;
      }};
    }}.testPercentile(3, 5, 50, Summary::byThroughput);
  }
}
