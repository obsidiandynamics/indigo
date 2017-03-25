package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public abstract class ActorSystemConfig {
  /** The number of threads for the dispatcher pool. This number is a guide only; the actual pool may
   *  be sized dynamically depending on the thread pool used. */
  protected int parallelism = get("indigo.parallelism", Integer::parseInt, 0);
  
  /** The default timeout when asking from outside the actor system. */
  protected int defaultAskTimeoutMillis = get("indigo.defaultAskTimeoutMillis", Integer::parseInt, 10 * 60_1000);
  
  public static enum ExecutorChoice implements Function<Integer, ExecutorService> {
    FORK_JOIN_POOL(Executors::newWorkStealingPool),
    FIXED_THREAD_POOL(Threads::prestartedFixedThreadPool);
    
    private final Function<Integer, ExecutorService> func;
    private ExecutorChoice(Function<Integer, ExecutorService> func) { this.func = func; }
    @Override public ExecutorService apply(Integer parallelism) { return func.apply(parallelism); }
  }
  
  /** Maps a given parallelism value to an appropriately sized thread pool. */
  protected Function<Integer, ExecutorService> executor = get("indigo.executor", ExecutorChoice::valueOf, ExecutorChoice.FORK_JOIN_POOL);
  
  protected ActorConfig defaultActorConfig = new ActorConfig() {};
  
  public final ActorSystem define() {
    return new ActorSystem(this);
  }
  
  final int getParallelism() {
    return parallelism > 0 ? parallelism : getNumProcessors() - parallelism;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}