package com.obsidiandynamics.indigo.topic;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.junit.Test;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.topic.TopicGen.*;

import junit.framework.*;

public final class TopicRouterBenchmark implements TestSupport {
  abstract static class Config implements Spec {
    ExecutorChoice executorChoice = null;
    long n;
    int threads;
    int bias;
    TopicGen topicGen;
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
  
  static TopicGen medium() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .add(new TopicSpec(1, 0, 0).nodes(2))
        .build();
  }
  
  @Test
  public void test() throws Exception {
    new Config() {{
      n = 30_000;
      threads = Runtime.getRuntime().availableProcessors();
      bias = 1_000;
      topicGen = medium();
      warmupFrac = .05f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
    }}.test();
  }
  
  private static class BenchSubscriber implements Subscriber {
    final Topic topic;
    //final Set<Topic> uniqueTopics = new HashSet<>();
    long received;

    BenchSubscriber(Topic topic) {
      this.topic = topic;
    }

    @Override
    public void accept(Delivery delivery) {
      //uniqueTopics.add(delivery.getTopic());
      received++;
    }
  }
  
  private Summary test(Config c) throws Exception {
    final ActorSystem system = new ActorSystemConfig() {{
      parallelism = c.threads;
      defaultActorConfig = new ActorConfig() {{ 
        bias = c.bias; 
      }};
    }}
    .createActorSystem()
    .addExecutor(r -> r.run()).named("current_thread")
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig() {{
      executorName = "current_thread";
    }}));
    
    final ActorRef routerRef = ActorRef.of(TopicActor.ROLE);
    final List<BenchSubscriber> subscribers = new ArrayList<>();
    
    c.getLog().out.format("subscribers: (-): %,d, (+): %,d, (#): %,d\n", 
                          c.topicGen.getExactInterests().size(),
                          c.topicGen.getSingleLevelWildcardInterests().size(),
                          c.topicGen.getMultiLevelWildcardInterests().size());
    
    final List<Interest> interests = c.topicGen.getAllInterests();
    for (Interest interest : interests) {
      for (int i = 0; i < interest.count; i++) {
        final BenchSubscriber subscriber = new BenchSubscriber(interest.topic);
        subscribers.add(subscriber);
        subscribe(system, routerRef, interest.topic, subscriber).get();
      }
    }

    final List<Topic> leafTopics = c.topicGen.getLeafTopics();
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
    
    system.shutdown();
    
    final Summary summary = new Summary();
    summary.timedOps = c.n;
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
