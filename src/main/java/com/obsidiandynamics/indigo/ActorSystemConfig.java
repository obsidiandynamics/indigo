package com.obsidiandynamics.indigo;

import java.util.concurrent.*;
import java.util.function.*;

public abstract class ActorSystemConfig {
  /** The number of threads for the dispatcher pool. This number is a guide only; the actual pool may
   *  be sized dynamically depending on the thread pool used. */
  protected int parallelism = PropertyUtils.get("indigo.parallelism", Integer::parseInt, 0);
  
  /** The total (system-wide) backlog at which point throttling is enforced. */
  protected long backlogCapacity = PropertyUtils.get("indigo.backlogCapacity", Long::parseLong, 100_000L);
  
  /** The time penalty for each throttling block. */
  protected int backlogThrottleMillis = PropertyUtils.get("indigo.backlogThrottleMillis", Integer::parseInt, 1);
  
  /** Upper bound on the number of consecutive blocks imposed during throttling. */
  protected int backlogThrottleTries = PropertyUtils.get("indigo.backlogThrottleTries", Integer::parseInt, 10);
  
  public static enum Executor implements Function<Integer, ExecutorService> {
    FORK_JOIN_POOL(Executors::newWorkStealingPool),
    FIXED_THREAD_POOL(Executors::newFixedThreadPool);
    
    private final Function<Integer, ExecutorService> func;
    private Executor(Function<Integer, ExecutorService> func) { this.func = func; }
    @Override public ExecutorService apply(Integer parallelism) { return func.apply(parallelism); }
  }
  
  /** Maps a given parallelism value to an appropriately sized thread pool. */
  protected Function<Integer, ExecutorService> executor = PropertyUtils.get("indigo.executor", Executor::valueOf, Executor.FORK_JOIN_POOL);
  
  protected ActorConfig defaultActorConfig = new ActorConfig() {};
  
  public final ActorSystem define() {
    return new ActorSystem(this);
  }
  
  int getParallelism() {
    return parallelism > 0 ? parallelism : getNumProcessors() - parallelism;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}