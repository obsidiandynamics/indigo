package com.obsidiandynamics.indigo;

public abstract class ActorSystemConfig {
  protected int numThreads = getDefaultThreads();
  
  protected long backlogCapacity = PropertyUtils.get("indigo.backlogCapacity", Long::parseLong, 10_000L);
  
  protected int backlogThrottleMillis = PropertyUtils.get("indigo.backlogThrottleMillis", Integer::parseInt, 1);
  
  protected int backlogThrottleTries = PropertyUtils.get("indigo.backlogThrottleTries", Integer::parseInt, 10);
  
  protected ActorConfig defaultActorConfig = new ActorConfig() {};
  
  public final ActorSystem define() {
    return new ActorSystem(this);
  }
  
  private static int getDefaultThreads() {
    final int numThreads = PropertyUtils.get("indigo.numThreads", Integer::parseInt, 0);
    return numThreads > 0 ? numThreads : getNumProcessors() - numThreads;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}
