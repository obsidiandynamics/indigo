package com.obsidiandynamics.indigo;

public abstract class ActorSystemConfig {
  /** The number of threads for the dispatcher pool. This number is a guide only; the actual pool may
   *  be sized dynamically. */
  protected int numThreads = getDefaultThreads();
  
  /** The total (system-wide) backlog at which point throttling is enforced. */
  protected long backlogCapacity = PropertyUtils.get("indigo.backlogCapacity", Long::parseLong, 100_000L);
  
  /** The time penalty for each throttling block. */
  protected int backlogThrottleMillis = PropertyUtils.get("indigo.backlogThrottleMillis", Integer::parseInt, 1);
  
  /** Upper bound on the number of consecutive blocks imposed during throttling. */
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
