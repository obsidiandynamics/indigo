package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class EgressBuilder<I, O> extends MessageBuilder {
  private Executor executor;
  
  private boolean parallel;

  EgressBuilder(Activation activation, Function<I, CompletableFuture<O>> func) {
    super(activation);
    serial().withCommonPool().target((body, requestId) -> {
      activation.stashIfTransitioning();
      if (parallel) {
        // execute directly on the given executor, with the response going back as a message
        // into the actor system
        executor.execute(() -> processEgress(func, body, requestId));
      } else {
        // execute using an egress agent within the fundamental rules of an actor system (such
        // full serialisation), but using the given executor (rather than the global executor)
        final Consumer<Activation> agent = a -> processEgress(func, body, requestId);
        final ActorRef egressRef = ActorRef.of(ActorRef.EGRESS, activation.ref.encode());
        activation.system.send(new Message(activation.ref, egressRef, agent, null, false), executor);
      }
    });
  }
  
  /**
   *  Enables strict serialisation, whereby tasks are executed in the order of submission
   *  and always one-at-a-time with respect to the enqueuing actor.<br><br>
   *  
   *  This is the default execution mode. Use {@link #parallel()} to override.
   *  
   *  @return This builder instance for chaining.
   */
  public EgressBuilder<I, O> serial() {
    parallel = false;
    return this;
  }
  
  /**
   *  Enables parallel execution, whereby tasks may be executed in parallel and in any order.
   *  
   *  @return This builder instance for chaining.
   */
  public EgressBuilder<I, O> parallel() {
    parallel = true;
    return this;
  }
  
  /**
   *  Configures the egress to use the common {@link ForkJoinPool}.<br><br>
   *  
   *  This is the default behaviour. Use {@link #withExecutor(Executor)} to override.
   *  
   *  @return This builder instance for chaining.
   */
  public EgressBuilder<I, O> withCommonPool() {
    return withExecutor(ForkJoinPool.commonPool());
  }

  /**
   *  Configures the egress to use a given executor for scheduling the egress task.
   *  
   *  @param executor The executor to use.
   *  @return This builder instance for chaining.
   */
  public EgressBuilder<I, O> withExecutor(Executor executor) {
    this.executor = executor;
    return this;
  }
  
  @SuppressWarnings("unchecked")
  private void processEgress(Function<I, CompletableFuture<O>> func, Object body, UUID requestId) {
    final CompletableFuture<O> future;
    try {
      future = func.apply((I) body);
    } catch (Throwable t) {
      handleError(requestId, t);
      return;
    }
    
    future.whenComplete((out, t) -> {
      if (t == null) {
        if (requestId != null) {
          final Message resp = new Message(null, activation.ref, out, requestId, true);
          activation.system.send(resp);
        }
      } else {
        handleError(requestId, t);
      }
    });
  }
  
  private void handleError(UUID requestId, Throwable t) {
    activation.actorConfig.exceptionHandler.accept(activation.system, t);
    final Fault fault = new Fault(FaultType.ON_EGRESS, null, t);
    activation.system.addToDeadLetterQueue(fault);
    if (requestId != null) {
      final Message resp = new Message(null, activation.ref, fault, requestId, true);
      activation.system.send(resp);
    }
  }
}