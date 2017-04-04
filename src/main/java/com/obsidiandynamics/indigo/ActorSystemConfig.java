package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.Key.*;
import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public abstract class ActorSystemConfig {
  public static final class Key {
    public static final String PARALLELISM = "indigo.system.parallelism";
    public static final String DEFAULT_ASK_TIMEOUT_MILLIS = "indigo.system.defaultAskTimeoutMillis";
    public static final String EXECUTOR = "indigo.system.executor";
    public static final String EXCEPTION_HANDLER = "indigo.system.exceptionHandler";
    public static final String DEAD_LETTER_QUEUE_SIZE = "indigo.system.deadLetterQueueSize";
  }
  
  /** The number of threads for the dispatcher pool. This number is a guide only; the actual pool may
   *  be sized dynamically depending on the thread pool used. */
  public int parallelism = get(PARALLELISM, Integer::parseInt, 0);
  
  /** The default timeout when asking from outside the actor system. */
  public int defaultAskTimeoutMillis = get(DEFAULT_ASK_TIMEOUT_MILLIS, Integer::parseInt, 1 * 60_000);
  
  public enum ExecutorChoice implements Function<Integer, ExecutorService> {
    FORK_JOIN_POOL(Executors::newWorkStealingPool),
    FIXED_THREAD_POOL(Threads::prestartedFixedThreadPool);
    
    private final Function<Integer, ExecutorService> func;
    private ExecutorChoice(Function<Integer, ExecutorService> func) { this.func = func; }
    @Override public ExecutorService apply(Integer parallelism) { return func.apply(parallelism); }
  }
  
  /** Maps a given parallelism value to an appropriately sized thread pool. */
  public Function<Integer, ExecutorService> executor = get(EXECUTOR, ExecutorChoice::valueOf, FORK_JOIN_POOL);
  
  public enum ExceptionHandlerChoice implements BiConsumer<ActorSystem, Throwable> {
    /** Forwards the exception to the system-level handler. Can only be used within the actor config. */
    SYSTEM((sys, t) -> sys.getConfig().exceptionHandler.accept(sys, t)),
    /** Prints the stack trace to the console. */
    CONSOLE((sys, t) -> t.printStackTrace()),
    /** Accumulates exceptions internally, throwing them from the <code>drain()</code> method. */
    DRAIN((sys, t) -> sys.addError(t));
    
    private final BiConsumer<ActorSystem, Throwable> handler;
    private ExceptionHandlerChoice(BiConsumer<ActorSystem, Throwable> handler) { this.handler = handler; }
    @Override public void accept(ActorSystem sys, Throwable t) { handler.accept(sys, t); }
  }
  
  /** Handles uncaught exceptions thrown from within an actor, where those exceptions aren't handled by the actor's
   *  own uncaught exception handler. */
  public BiConsumer<ActorSystem, Throwable> exceptionHandler = get(EXCEPTION_HANDLER, ExceptionHandlerChoice::valueOf, CONSOLE);
  
  /** Upper bound on the size of the DQL. Beyond this, truncation from the head (least recent) occurs. */
  public int deadLetterQueueSize = get(DEAD_LETTER_QUEUE_SIZE, Integer::parseInt, 10_000);
  
  public Diagnostics diagnostics = new Diagnostics() {{
    traceEnabled = false;
  }};
  
  /** The default actor configuration. */
  public ActorConfig defaultActorConfig = new ActorConfig() {};
  
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