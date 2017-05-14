package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class EgressBuilder<I, O> {
  private final Activation activation;
  
  private final Function<I, CompletableFuture<O>> func;
  
  private boolean parallel = false;

  EgressBuilder(Activation activation, Function<I, CompletableFuture<O>> func) {
    this.activation = activation;
    this.func = func;
  }
  
  /**
   *  Enables strict serialisation, whereby tasks are executed in the order of submission
   *  and always one-at-a-time with respect to the enqueuing actor.<br><br>
   *  
   *  This is the default execution mode. Use {@link #parallel()} to override.
   */
  public void serial() {
    parallel = false;
  }
  
  /**
   *  Enables parallel execution, whereby tasks may be executed in parallel and in any order.
   */
  public void parallel() {
    parallel = true;
  }
  
  public MessageBuilder withCommonPool() {
    return withExecutor(ForkJoinPool.commonPool());
  }

  public MessageBuilder withExecutor(Executor executor) {
    return new MessageBuilder(activation, (body, requestId) -> {
      activation.stashIfTransitioning();
      if (parallel) {
        // execute directly on the given executor, with the response going back as a message
        // into the actor system
        executor.execute(() -> processEgress(body, requestId));
      } else {
        // execute using an egress agent within the fundamental rules of an actor system (such
        // full serialisation), but using the given executor (rather than the global executor)
        final Consumer<Activation> agent = a -> processEgress(body, requestId);
        final ActorRef egressRef = ActorRef.of(ActorRef.EGRESS, activation.ref.encode());
        activation.system.send(new Message(activation.ref, egressRef, agent, null, false), executor);
      }
    });
  }
  
  @SuppressWarnings("unchecked")
  private void processEgress(Object body, UUID requestId) {
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