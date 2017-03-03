package com.obsidiandynamics.indigo;

public abstract class ActorSystemConfig {
  /** Number of threads to use for the dispatch pool. While it is allowed, the number of threads should not
   *  be fewer than two. This leaves at least one thread for ingress, which could be throttled while injecting
   *  messages into the actor system. */
  protected int numThreads = getDefaultThreads();
  
  protected long backlogCapacity = PropertyUtils.get("indigo.backlogCapacity", Long::parseLong, 10_000L);
  
  protected int backlogThrottleMillis = PropertyUtils.get("indigo.backlogThrottleMillis", Integer::parseInt, 10);
  
  protected ActorConfig defaultActorConfig = new ActorConfig() {};
  
  public final ActorSystem define() {
    return new ActorSystem(this);
  }
  
  private static int getDefaultThreads() {
    final int numThreads = PropertyUtils.get("indigo.actorThreads", Integer::parseInt, 0);
    return numThreads > 0 ? numThreads : getNumProcessors() - numThreads;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}
