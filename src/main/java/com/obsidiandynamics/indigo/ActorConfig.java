package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorConfig.ActivationChoice.*;
import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.util.function.*;

import com.obsidiandynamics.indigo.activation.*;

public abstract class ActorConfig {
  /** The number of consecutive turns an actor is accorded before releasing its thread. */
  public int bias = get("indigo.bias", Integer::parseInt, 1);
  
  /** The backlog level at which point throttling is enforced. Set to <code>Integer.MAX_VALUE</code> to
   *  avoid throttling. */
  public long backlogThrottleCapacity = get("indigo.backlogThrottleCapacity", Long::parseLong, 10_000L);
  
  /** The time penalty for each throttling block. */
  public int backlogThrottleMillis = get("indigo.backlogThrottleMillis", Integer::parseInt, 1);
  
  /** Upper bound on the number of consecutive penalties imposed during throttling, after which the message
   *  will be enqueued even if the backlog is over capacity. */
  public int backlogThrottleTries = get("indigo.backlogThrottleTries", Integer::parseInt, 10);

  public static enum ActivationChoice implements ActivationFactory {
    SYNC_QUEUE(SyncQueueActivation::new),
    NODE_QUEUE(NodeQueueActivation::new),;
    
    private final ActivationFactory factory;
    private ActivationChoice(ActivationFactory factory) { this.factory = factory; }
    @Override public Activation create(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
      return factory.create(id, ref, system, actorConfig, actor);
    }
  }
  
  public ActivationFactory activationFactory = get("indigo.activationFactory", ActivationChoice::valueOf, NODE_QUEUE);
  
  public BiConsumer<ActorSystem, Throwable> exceptionHandler = ActorSystemConfig.ExceptionHandlerChoice.SYSTEM;
}
