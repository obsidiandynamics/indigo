package com.obsidiandynamics.indigo;

public abstract class ActorSystemConfig {
  protected int numThreads = getDefaultThreads();
  
  protected long backlogCapacity = PropertyUtils.get("indigo.backlogCapacity", Long::parseLong, 100_000L);
  
  protected int backlogThrottleMillis = PropertyUtils.get("indigo.backlogThrottleMillis", Integer::parseInt, 10);
  
  protected ActorConfig defaultActorConfig = new ActorConfig() {};
  
  public final ActorSystem define() {
    return new ActorSystem(this);
  }
  
  private static int getDefaultThreads() {
    final int defInt = PropertyUtils.get("indigo.actorThreads", Integer::parseInt, 0);
    return defInt > 0 ? defInt : getNumProcessors() - defInt;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}
