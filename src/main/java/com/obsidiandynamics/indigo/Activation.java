package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActivationState.*;
import static com.obsidiandynamics.indigo.FaultType.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public abstract class Activation {
  private final long id;
  
  protected final ActorRef ref;
  
  protected final ActorSystem system;
  
  protected final ActorConfig actorConfig;
  
  private final Actor actor;
  
  private final Executor executor;
  
  private final Reaper reaper;
  
  protected final Map<UUID, PendingRequest> pending = new HashMap<>();
  
  /** Current state of the activation. */
  private ActivationState state = PASSIVATED;
  
  private Stash stash;
  
  private boolean passivationScheduled;
  
  private long requestCounter;
  
  private Object faultReason;
  
  private Message activatingMessage;
  
  private volatile long lastMessageTime;
  
  protected Activation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor, Executor executor) {
    this.id = id;
    this.ref = ref;
    this.system = system;
    this.actorConfig = actorConfig;
    this.actor = actor;
    this.executor = executor;
    
    if (actorConfig.reapTimeoutMillis != 0 && system.getReaper().isReapingEnabled()) {
      reaper = system.getReaper();
      lastMessageTime = System.currentTimeMillis();
      reaper.register(this);
    } else {
      reaper = null;
    }
  }
  
  abstract boolean enqueue(Message m);
  
  final void dispose() {
    if (reaper != null) {
      reaper.deregister(this);
    }
  }
  
  final long getLastMessageTime() {
    return lastMessageTime;
  }
  
  protected final void dispatch(Runnable r) {
    executor.execute(() -> {
      try {
        r.run();
      } catch (Throwable t) {
        system.getConfig().exceptionHandler.accept(system, t);
        system.terminate();
      }
    });
  }
  
  final long getId() {
    return id;
  }
  
  final long getAndIncrementRequestCounter() {
    if (requestCounter == 0) {
      // lazy assignment of the request counter's initial value
      requestCounter = Crypto.machineRandom();
    }
    return requestCounter++;
  }
  
  public final ActorRef self() {
    return ref;
  }
  
  public final void passivate() {
    passivationScheduled = true;
  }
  
  public final void unpassivate() {
    passivationScheduled = false;
  }
  
  public final void fault(Object reason) {
    faultReason = reason;
  }
  
  public final void propagateFault(Fault fault) {
    fault(fault.getReason());
  }
  
  public final void messageFault(Object body) {
    fault(body != null ? "Cannot handle message body of type " + body.getClass().getName() : "Cannot handle null message body");
  }
  
  public final MessageBuilder to(ActorRef to) {
    return new MessageBuilder(this).target((body, requestId) -> send(new Message(ref, to, body, requestId, false)));
  }
  
  public final <I> EgressBuilder<I, Void> egress(Consumer<I> consumer) {
    return egress(in -> {
      consumer.accept(in);
      return null;
    });
  }
  
  public final <I> EgressBuilder<I, Void> egress(Runnable runnable) {
    return egress(in -> {
      if (in != null) throw new IllegalArgumentException("Cannot pass a value to this egress lambda");
      
      runnable.run();
      return null;
    });
  }
  
  public final <O> EgressBuilder<Object, O> egress(Supplier<O> supplier) {
    return egress(in -> {
      if (in != null) throw new IllegalArgumentException("Cannot pass a value to this egress lambda");
      
      return supplier.get();
    });
  }
  
  public final <I, O> EgressBuilder<I, O> egress(Function<I, O> func) {
    return egressAsync(i -> CompletableFuture.completedFuture(func.apply(i)));
  }
  
  public final <I, O> EgressBuilder<I, O> egressAsync(Supplier<CompletableFuture<O>> supplier) {
    return egressAsync(in -> {
      if (in != null) throw new IllegalArgumentException("Cannot pass a value to this egress lambda");
      
      return supplier.get();
    });
  }
  
  public final <I, O> EgressBuilder<I, O> egressAsync(Function<I, CompletableFuture<O>> func) {
    return new EgressBuilder<>(this, func);
  }
  
  public final MessageBuilder toSelf() {
    return to(ref);
  }
  
  public final MessageBuilder toSenderOf(Message m) {
    return to(m.from());
  }
  
  public final ReplyBuilder reply(Message m) {
    return new ReplyBuilder(this, m);
  }
  
  public final ForwardBuilder forward(Message m) {
    return new ForwardBuilder(this, m);
  }
  
  public final void send(Message message) {
    if (message.requestId() != null && ! message.isFault()) {
      stashIfTransitioning();
    }
    system.send(message);
  }
  
  final void stashIfTransitioning() {
    final ActivationState stateCache = state;
    if (stateCache == ACTIVATING || stateCache == PASSIVATING) {
      _stash(Functions::alwaysTrue);
    }
  }
  
  private void cancelTimeout(PendingRequest req) {
    if (req.getTimeoutTask() != null) {
      system.getTimeoutScheduler().abort(req.getTimeoutTask());
    }
  }
  
  private void clearPending() {
    for (PendingRequest req : pending.values()) {
      cancelTimeout(req);
    }
    pending.clear();
  }
  
  private Fault raiseFault(FaultType type, Message originalMessage) {
    final Fault fault = new Fault(type, originalMessage, faultReason);
    if (originalMessage != null && originalMessage.requestId() != null && ! originalMessage.isResponse()) {
      send(new Message(ref, originalMessage.from(), fault, originalMessage.requestId(), true));
    }
    system.addToDeadLetterQueue(fault);
    faultReason = null;
    return fault;
  }
  
  private Fault checkAndRaiseFault(FaultType type, Message originalMessage) {
    if (faultReason != null) {
      return raiseFault(type, originalMessage);
    } else {
      return null;
    }
  }
  
  private boolean ensureActivated(Message m) {
    if (state == PASSIVATED) {
      setState(ACTIVATING);
      try {
        assert diagnostics().traceMacro("A.ensureActivated: m=%s", m);
        actor.activated(this);
      } catch (Throwable t) {
        fault(t);
        actorConfig.exceptionHandler.accept(system, t);
      }
      
      if (faultReason != null) {
        clearPending();
        _unstash();
        raiseFault(ON_ACTIVATION, m);
        setState(PASSIVATED);
        return false;
      } else if (pending.isEmpty()) {
        setState(ACTIVATED);
        return true;
      } else {
        activatingMessage = m;
        return true;
      }
    } else {
      return true;
    }
  }
  
  protected ActivationState getState() {
    return state;
  }
  
  private void setState(ActivationState state) {
    this.state = state;
  }
  
  protected final void passivateIfScheduled() {
    if (actorConfig.ephemeral) {
      passivationScheduled = true;
    }
    
    if (passivationScheduled && state == ACTIVATED && pending.isEmpty() && stash == null) {
      assert diagnostics().traceMacro("A.passivateIfScheduled: passivating ref=%s", ref);
      
      passivationScheduled = false;
      setState(PASSIVATING);
      try {
        actor.passivated(this);
      } catch (Throwable t) {
        fault(t);
        actorConfig.exceptionHandler.accept(system, t);
      }
      
      if (faultReason != null) {
        clearPending();
        _unstash();
        raiseFault(ON_PASSIVATION, null);
        setState(ACTIVATED);
      } else if (pending.isEmpty()) {
        setState(PASSIVATED);
      }
    }
  }
  
  protected final void processMessage(Message m) {
    if (reaper != null) {
      lastMessageTime = System.currentTimeMillis();
    }
    
    if (m.isResponse()) {
      assert diagnostics().traceMacro("A.processMessage: solicited m=%s", m);
      processSolicited(m);
    } else {
      if (! ensureActivated(m)) {
        return;
      }
      assert diagnostics().traceMacro("A.processMessage: unsolicited m=%s", m);
      processUnsolicited(m);
    }
    
    if (stash != null && stash.unstashing) {
      assert diagnostics().traceMacro("A.processMessage: unstashing");
      while (! stash.messages.isEmpty() && ! ensureActivated(stash.messages.get(0))) {
        stash.messages.remove(0);
      }
      
      while (stash.unstashing && ! stash.messages.isEmpty()) {
        final Message stashed = stash.messages.remove(0);
        processUnsolicited(stashed);
      }
      
      if (stash.messages.isEmpty()) {
        stash = null;
      }
    }
  }
  
  private void processSolicited(Message m) {
    final PendingRequest req = pending.remove(m.requestId());
    final Object body = m.body();
    Fault fault = null;
    if (body instanceof Signal) {
      if (body instanceof Timeout) {
        if (req != null) {
          try {
            req.getOnTimeout().run();
          } catch (Throwable t) {
            fault(t);
            actorConfig.exceptionHandler.accept(system, t);
          } finally {
            fault = checkAndRaiseFault(ON_TIMEOUT, m);
          }
        }
      } else if (body instanceof Fault) {
        if (req != null) {
          cancelTimeout(req);
          if (req.getOnFault() != null) {
            try {
              req.getOnFault().accept((Fault) body);
            } catch (Throwable t) {
              fault(t);
              actorConfig.exceptionHandler.accept(system, t);
            } finally {
              fault = checkAndRaiseFault(ON_FAULT, m);
            }
          }
        }
      } else {
        throw new FrameworkError("Unsupported signal of type " + body.getClass().getName());
      }
    } else if (req != null) {
      cancelTimeout(req);
      try {
        req.getOnResponse().accept(m);
      } catch (Throwable t) {
        fault(t);
        actorConfig.exceptionHandler.accept(system, t);
      } finally {
        fault = checkAndRaiseFault(ON_RESPONSE, m);
      }
    }
    
    if (fault != null) {
      switch (state) {
        case ACTIVATING:
          clearPending();
          _unstash();
          // during asynchronous activation, the message causing the activation gets stashed - treat as
          // offending and remove it
          stash.messages.remove(0);
          setState(PASSIVATED);
          fault(fault.getReason());
          raiseFault(ON_ACTIVATION, activatingMessage);
          activatingMessage = null;
          break;
          
        case PASSIVATING:
          clearPending();
          _unstash();
          setState(ACTIVATED);
          fault(fault.getReason());
          raiseFault(ON_PASSIVATION, null);
          break;
          
        default:
          break;
      }      
    } else if (pending.isEmpty()) {
      switch (state) {
        case ACTIVATING:
          _unstash();
          activatingMessage = null;
          setState(ACTIVATED);
          break;
          
        case PASSIVATING:
          _unstash();
          setState(PASSIVATED);
          break;
          
        default:
          break;
      }
    }
  }
  
  private void processUnsolicited(Message m) {
    if (stash != null && ! stash.unstashing && stash.filter.test(m)) {
      stash.messages.add(m);
    } else if (m.body() instanceof Signal) {
      final Signal signal = m.body();
      if (signal instanceof SleepingPill) {
        passivate();
      } else {
        throw new FrameworkError("Unsupported signal of type " + signal.getClass().getName());
      }
    } else {
      try {
        actor.act(this, m);
      } catch (Throwable t) {
        fault(t);
        actorConfig.exceptionHandler.accept(system, t);
      } finally {
        checkAndRaiseFault(ON_ACT, m);
      }
    }
  }

  /**
   *  Stashes all unsolicited messages that test <code>true</code> to the given filter.<p>
   *  
   *  This method can only be called from the act method (or lambda); it cannot be called during
   *  activation or passivation. (Stashing is implicit during a state transition.)<p>
   *  
   *  This method is idempotent; calling it twice with the same filter has no further affect.
   *  Calling it with a different filter will replace the acting filter while preserving the 
   *  existing stash.
   *  
   *  @param filter The filter.
   */
  public final void stash(Predicate<Message> filter) {
    switch (state) {
      case ACTIVATING:
      case PASSIVATING:
        throw new IllegalStateException("Cannot stash during a life-cycle transition");
        
      default:
        _stash(filter);
        break;
    }
  }
  
  /**
   *  Unstashes all previously stashed messages.<p>
   *  
   *  This method can only be called from the act method (or lambda); it cannot be called during
   *  activation or passivation. (Stashing is implicit during a state transition.)<p>
   *  
   *  This method is idempotent; calling it twice has no further affect.
   */
  public final void unstash() {
    switch (state) {
      case ACTIVATING:
      case PASSIVATING:
        throw new IllegalStateException("Cannot unstash during a life-cycle transition");
        
      default:
        _unstash();
        break;
    }
  }
  
  private void _stash(Predicate<Message> filter) {
    assert diagnostics().traceMacro("A._stash: ref=%s", ref);
    
    if (stash != null) {
      stash.unstashing = false;
    } else {
      stash = new Stash();
    }
    stash.filter = filter;
  }
  
  private void _unstash() {
    assert diagnostics().traceMacro("A._unstash: ref=%s", ref);
    
    if (stash == null) return;
    stash.unstashing = true;
  }
  
  public final Diagnostics diagnostics() {
    return system.getConfig().diagnostics;
  }
  
  @Override
  public final String toString() {
    return "Activation [ref=" + ref + "]";
  }
}
