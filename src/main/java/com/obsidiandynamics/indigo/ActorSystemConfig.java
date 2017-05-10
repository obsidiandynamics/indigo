package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.ExecutorChoice.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.Key.*;
import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public class ActorSystemConfig {
  public static final class Key {
    public static final String PARALLELISM = "indigo.system.parallelism";
    public static final String INGRESS_COUNT = "indigo.system.ingressCount";
    public static final String DEFAULT_ASK_TIMEOUT_MILLIS = "indigo.system.defaultAskTimeoutMillis";
    public static final String EXECUTOR = "indigo.system.executor";
    public static final String EXCEPTION_HANDLER = "indigo.system.exceptionHandler";
    public static final String DEAD_LETTER_QUEUE_SIZE = "indigo.system.deadLetterQueueSize";
    private Key() {}
  }
  
  /** The number of threads for the dispatcher pool. This number is a guide only; the actual pool may
   *  be sized dynamically depending on the thread pool used.<p>
   *  
   *  Positive number: number of threads;
   *  Zero: set to the number of cores on the machine;
   *  Negative number: subtracts the number from the number of cores, so a -1 on an 8-core machine gives 7 threads. */
  public int parallelism = get(PARALLELISM, Integer::parseInt, 0);
  
  /** The number of ingress actors to use.<p>
   *  
   *  Positive number: number of actors;
   *  Zero: set to the number of cores on the machine;
   *  Negative number: subtracts the number from the number of cores, so a -1 on an 8-core machine gives 7 actors. */
  public int ingressCount = get(INGRESS_COUNT, Integer::parseInt, 0);
  
  /** The default timeout when asking from outside the actor system. */
  public int defaultAskTimeoutMillis = get(DEFAULT_ASK_TIMEOUT_MILLIS, Integer::parseInt, 60_000);
  
  public static final class ExecutorParams {
    public final int parallelism;
    public final JvmVersion version;
    
    public ExecutorParams(int parallelism, JvmVersion version) {
      this.parallelism = parallelism;
      this.version = version;
    }
  }
  
  public enum ExecutorChoice implements Function<ExecutorParams, ExecutorService> {
    AUTO(params -> Threads.autoPool(params.parallelism, params.version)),
    FORK_JOIN_POOL(params -> Threads.cappedForkJoinPool(params.parallelism)),
    FIXED_THREAD_POOL(params -> Threads.prestartedFixedThreadPool(params.parallelism));
    
    private final Function<ExecutorParams, ExecutorService> func;
    private ExecutorChoice(Function<ExecutorParams, ExecutorService> func) { this.func = func; }
    @Override public ExecutorService apply(ExecutorParams params) { return func.apply(params); }
  }
  
  /** Maps a given parallelism value to an appropriately sized thread pool. */
  public Function<ExecutorParams, ExecutorService> executor = get(EXECUTOR, ExecutorChoice::valueOf, AUTO);
  
  /** The default error stream to use. */
  public PrintStream err = System.err;
  
  public enum ExceptionHandlerChoice implements BiConsumer<ActorSystem, Throwable> {
    /** Forwards the exception to the system-level handler. Can only be used within the actor config. */
    SYSTEM((sys, t) -> sys.getConfig().exceptionHandler.accept(sys, t)),
    /** Prints the stack trace to {@link System#err}. */
    CONSOLE((sys, t) -> t.printStackTrace(sys.getConfig().err)),
    /** Accumulates exceptions internally, throwing them from the {@link ActorSystem#drain} method. */
    DRAIN((sys, t) -> sys.addError(t)),
    /** Combination of both {@link #CONSOLE} and {@link #DRAIN}. */
    CONSOLE_DRAIN(CONSOLE.andThen(DRAIN));
    
    private final BiConsumer<ActorSystem, Throwable> handler;
    private ExceptionHandlerChoice(BiConsumer<ActorSystem, Throwable> handler) { this.handler = handler; }
    @Override public void accept(ActorSystem sys, Throwable t) { handler.accept(sys, t); }
  }
  
  /** Handles uncaught exceptions thrown from within an actor, where those exceptions aren't handled by the actor's
   *  own uncaught exception handler. */
  public BiConsumer<ActorSystem, Throwable> exceptionHandler = get(EXCEPTION_HANDLER, ExceptionHandlerChoice::valueOf, CONSOLE_DRAIN);
  
  /** Upper bound on the size of the DQL. Beyond this, truncation from the head (least recent) occurs. */
  public int deadLetterQueueSize = get(DEAD_LETTER_QUEUE_SIZE, Integer::parseInt, 10_000);
  
  /** In-memory diagnostics. */
  public Diagnostics diagnostics = new Diagnostics() {};
  
  /** The default actor configuration. */
  public ActorConfig defaultActorConfig = new ActorConfig() {};
  
  public final ActorSystem createActorSystem() {
    return ActorSystem.create(this);
  }
  
  final int getParallelism() {
    return parallelism > 0 ? parallelism : getNumProcessors() - parallelism;
  }
  
  final int getIngressCount() {
    return ingressCount > 0 ? ingressCount : getNumProcessors() - ingressCount;
  }
  
  final void init() {
    if (exceptionHandler == SYSTEM) {
      throw new IllegalArgumentException(String.format("Cannot use %s.%s as a top level exception handler", 
                                                       ExceptionHandlerChoice.class.getSimpleName(), SYSTEM));
    }
    diagnostics.init();
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
  }
}