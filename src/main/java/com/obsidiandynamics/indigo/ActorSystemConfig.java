package com.obsidiandynamics.indigo;

public abstract class ActorSystemConfig {
  protected int numThreads = getDefaultThreads();
  
  protected long backlogCapacity = 100_000;
  
  protected int backlogBackoffMillis = 10;
  
  public final ActorSystem define() {
    return new ActorSystem(this);
  }
  
  private static int getDefaultThreads() {
    final String defStr = System.getProperty("indigo.actorThreads", "0");
    final int defInt = Integer.parseInt(defStr);
    return defInt > 0 ? defInt : getNumProcessors() - defInt;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}
