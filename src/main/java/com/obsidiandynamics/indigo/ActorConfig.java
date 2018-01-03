package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorConfig.ActivationChoice.*;
import static com.obsidiandynamics.indigo.ActorConfig.Key.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.ActorSystemConfig.*;

public class ActorConfig {
  public static final class Key {
    public static final String BIAS = "indigo.actor.bias";
    public static final String BACKLOG_THROTTLE_CAPACITY = "indigo.actor.backlogThrottleCapacity";
    public static final String BACKLOG_THROTTLE_MILLIS = "indigo.actor.backlogThrottleMillis";
    public static final String BACKLOG_THROTTLE_TRIES = "indigo.actor.backlogThrottleTries";
    public static final String ACTIVATION_FACTORY = "indigo.actor.activationFactory";
    public static final String EXCEPTION_HANDLER = "indigo.actor.exceptionHandler";
    public static final String REAP_TIMEOUT_MILLIS = "indigo.actor.reapTimeoutMillis";
    public static final String EPHEMERAL = "indigo.actor.ephemeral";
    private Key() {}
  }
  
  /** The number of consecutive turns an actor is accorded before releasing its thread. */
  public int bias = get(BIAS, Integer::parseInt, 1);
  
  /** The backlog level at which point throttling is enforced. Set to <code>Long.MAX_VALUE</code> to
   *  avoid throttling. */
  public long backlogThrottleCapacity = get(BACKLOG_THROTTLE_CAPACITY, Long::parseLong, 10_000L);
  
  /** The time penalty for each throttling block. */
  public int backlogThrottleMillis = get(BACKLOG_THROTTLE_MILLIS, Integer::parseInt, 1);
  
  /** Upper bound on the number of consecutive penalties imposed during throttling, after which the message
   *  will be enqueued even if the backlog is over capacity. */
  public int backlogThrottleTries = get(BACKLOG_THROTTLE_TRIES, Integer::parseInt, 10);

  public enum ActivationChoice implements ActivationFactory {
    SYNC_QUEUE(SyncQueueActivation::new),
    NODE_QUEUE(NodeQueueActivation::new);
    
    private final ActivationFactory factory;
    private ActivationChoice(ActivationFactory factory) { this.factory = factory; }
    @Override public Activation create(long id, ActorRef ref, ActorSystem system, 
                                       ActorConfig actorConfig, Actor actor, Executor executor) {
      return factory.create(id, ref, system, actorConfig, actor, executor);
    }
  }
  
  /** Factory for creating activations. */
  public ActivationFactory activationFactory = get(ACTIVATION_FACTORY, ActivationChoice::valueOf, NODE_QUEUE);
  
  /** Handles uncaught exceptions thrown from within an actor. */
  public BiConsumer<ActorSystem, Throwable> exceptionHandler = get(EXCEPTION_HANDLER, ExceptionHandlerChoice::valueOf, SYSTEM);
  
  /** The lower bound on the number of milliseconds elapsed since the last message to the actor before it 
   *  becomes a candidate for reaping. Leave at <code>0</code> (default) to disable reaping. */
  public int reapTimeoutMillis = get(REAP_TIMEOUT_MILLIS, Integer::parseInt, 0);
  
  /** Whether this actor is ephemeral, i.e. the actor is used to process sporadic messages and has no need
   *  to remain in an activated state. An ephemeral actor will request passivation immediately after processing
   *  its message load. */
  public boolean ephemeral = get(EPHEMERAL, Boolean::parseBoolean, false);
}
