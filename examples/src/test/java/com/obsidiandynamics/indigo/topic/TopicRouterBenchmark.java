package com.obsidiandynamics.indigo.topic;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.benchmark.*;

public final class TopicRouterBenchmark implements TestSupport {
  abstract static class Config implements Spec {
    ExecutorChoice executorChoice = null;
    long n;
    int threads;
    int actors;
    int bias;
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
      return String.format("%d threads, %,d receive actors, %,d messages/actor, %.0f%% warmup fraction", 
                           threads, actors, n, warmupFrac * 100);
    }

    @Override
    public Summary run() {
      return new TopicRouterBenchmark().test(this);
    }
  }  
  
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = ActorSystem.create()
    .addExecutor(r -> r.run()).named("current_thread")
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig() {{
      executorName = "current_thread";
    }}));
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  static TopicGen tiny() {
    return TopicGen.builder()
        .add(new TopicSpec(1, 0, 0).nodes(1))
        .build();
  }
  
  @Test
  public void test() {
    new Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = 4;
      bias = 1_000;
      n = 1_000;
      warmupFrac = .05f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
    }}.test();
  }
  
  private static class BenchSubscriber implements Subscriber {
    final Topic topic;
    final Set<Topic> uniqueTopics = new HashSet<>();

    BenchSubscriber(Topic topic) {
      this.topic = topic;
    }

    @Override
    public void accept(Delivery delivery) {
      uniqueTopics.add(delivery.getTopic());
    }
  }
  
  private Summary test(Config c) {
    final List<BenchSubscriber> subscribers = new ArrayList<>();
    final Subscriber sub = new BenchSubscriber(null);
    
    final long took = 1000; //TODO
    final Summary summary = new Summary();
    summary.timedOps = c.n;
    summary.avgTime = took;
    return summary;
  }
  
  private CompletableFuture<SubscribeResponse> subscribe(String topic, Subscriber subscriber) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Subscribe(Topic.of(topic), subscriber));
  }
  
  private CompletableFuture<UnsubscribeResponse> unsubscribe(String topic, Subscriber subscriber) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Unsubscribe(Topic.of(topic), subscriber));
  }
}
