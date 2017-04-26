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
  
  protected final Map<UUID, PendingRequest> pending = new HashMap<>();
  
  /** Current state of the activation. */
  private ActivationState state = PASSIVATED;
  
  private Stash stash;
  
  private boolean passivationScheduled;
  
  private long requestCounter = Crypto.machineRandom();
  
  private Object faultReason;
  
  private Message activatingMessage;
  
  protected Activation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    this.id = id;
    this.ref = ref;
    this.system = system;
    this.actorConfig = actorConfig;
    this.actor = actor;
  }
  
  public abstract boolean enqueue(Message m);
  
  public final ActorRef self() {
    return ref;
  }
  
  public final void passivate() {
    passivationScheduled = true;
  }
  
  public final void fault(Object reason) {
    faultReason = reason;
  }
  
  @FunctionalInterface
  private interface MessageTarget {
    void send(Object body, UUID requestId);
  }
  
  public final class MessageBuilder {
    private final MessageTarget target;
    
    private int copies = 1;
    
    private Object requestBody;
    
    private long timeoutMillis;
    
    private Runnable onTimeout;
    
    private Consumer<Fault> onFault;
    
    private TimeoutTask timeoutTask;
    
    MessageBuilder(MessageTarget target) {
      this.target = target;
    }
    
    public MessageBuilder times(int copies) {
      this.copies = copies;
      return this;
    }
    
    public void tell(Object body) {
      for (int i = copies; --i >= 0;) {
        target.send(body, null);
      }
    }
    
    public MessageBuilder ask(Object requestBody) {
      this.requestBody = requestBody;
      return this;
    }
    
    public MessageBuilder ask() {
      return ask(null);
    }
    
    public MessageBuilder await(long timeoutMillis) {
      this.timeoutMillis = timeoutMillis;
      return this;
    }
    
    public MessageBuilder onTimeout(Runnable onTimeout) {
      this.onTimeout = onTimeout;
      return this;
    }
    
    public MessageBuilder onFault(Consumer<Fault> onFault) {
      this.onFault = onFault;
      return this;
    }
    
    public void onResponse(Consumer<Message> onResponse) {
      if (timeoutMillis != 0 ^ onTimeout != null) {
        throw new IllegalArgumentException("Only one of the timeout time or handler has been set");
      }
      
      for (int i = copies; --i >= 0;) {
        final UUID requestId = new UUID(id, requestCounter++);
        final PendingRequest req = new PendingRequest(onResponse, onTimeout, onFault);
        target.send(requestBody, requestId);
        pending.put(requestId, req);
        
        if (timeoutMillis != 0) {
          timeoutTask = new TimeoutTask(System.nanoTime() + timeoutMillis * 1_000_000l,
                                        requestId,
                                        ref,
                                        req);
          req.setTimeoutTask(timeoutTask);
          system.getTimeoutWatchdog().schedule(timeoutTask);
        }
      }
    }
    
    TimeoutTask getTimeoutTask() {
      return timeoutTask;
    }
    
    public void tell() {
      tell(null);
    }
  }
  
  public final MessageBuilder to(ActorRef to) {
    return new MessageBuilder((body, requestId) -> send(new Message(ref, to, body, requestId, false)));
  }
  
  public final class EgressBuilder<I, O> {
    private final Function<I, O> func;

    EgressBuilder(Function<I, O> func) {
      this.func = func;
    }

    @SuppressWarnings("unchecked")
    public MessageBuilder using(Executor executor) {
      return new MessageBuilder((body, requestId) -> {
        stashIfTransitioning();
        executor.execute(() -> {
          final O out;
          try {
            out = func.apply((I) body);
          } catch (Throwable t) {
            actorConfig.exceptionHandler.accept(system, t);
            final Fault fault = new Fault(ON_EGRESS, null, t);
            addToDeadLetterQueue(fault);
            if (requestId != null) {
              final Message resp = new Message(null, ref, fault, requestId, true);
              system.send(resp);
            }
            return;
          }
          
          if (requestId != null) {
            final Message resp = new Message(null, ref, out, requestId, true);
            system.send(resp);
          }
        });
      });
    }
  }
  
  public final <I, O> EgressBuilder<I, O> egress(Function<I, O> func) {
    return new EgressBuilder<>(func);
  }
  
  public final <I> EgressBuilder<I, Void> egress(Consumer<I> consumer) {
    return new EgressBuilder<>(in -> {
      consumer.accept(in);
      return null;
    });
  }
  
  public final <I> EgressBuilder<I, Void> egress(Runnable runnable) {
    return new EgressBuilder<>(in -> {
      if (in != null) throw new IllegalArgumentException("Cannot pass a value to this egress lambda");
      
      runnable.run();
      return null;
    });
  }
  
  public final <O> EgressBuilder<Object, O> egress(Supplier<O> supplier) {
    return new EgressBuilder<>(in -> {
      if (in != null) throw new IllegalArgumentException("Cannot pass a value to this egress lambda");
      
      return supplier.get();
    });
  }
  
  public final MessageBuilder toSelf() {
    return to(ref);
  }
  
  public final MessageBuilder toSenderOf(Message m) {
    return to(m.from());
  }
  
  public final class ReplyBuilder {
    private final Message m;
    
    ReplyBuilder(Message m) { this.m = m; }
    
    public void tell() {
      tell(null);
    }
    
    public void tell(Object responseBody) {
      final boolean isResponse = m.requestId() != null;
      // Solicited responses go through, but unsolicited ones are silently dropped. This relieves
      // services from having to program defensively, only sending replies when the consumer has
      // requested them with an <code>ask()</code>. It also prevents a consumer using <code>tell()</code>
      // from receiving an unsolicited message.
      if (isResponse) {
        send(new Message(ref, m.from(), responseBody, m.requestId(), true));
      }
    }
  }
  
  public final ReplyBuilder reply(Message m) {
    return new ReplyBuilder(m);
  }
  
  public final class ForwardBuilder {
    private final Message m;
    
    ForwardBuilder(Message m) { this.m = m; }
    
    public void to(ActorRef to) {
      send(new Message(m.from(), to, m.body(), m.requestId(), m.isResponse()));
    }
  }
  
  public final ForwardBuilder forward(Message m) {
    return new ForwardBuilder(m);
  }
  
  public final void send(Message message) {
    if (message.requestId() != null && ! message.isFault()) {
      stashIfTransitioning();
    }
    system.send(message);
  }
  
  private void stashIfTransitioning() {
    final ActivationState stateCache = state;
    if (stateCache == ACTIVATING || stateCache == PASSIVATING) {
      _stash(Functions::alwaysTrue);
    }
  }
  
  private void cancelTimeout(PendingRequest req) {
    if (req.getTimeoutTask() != null) {
      system.getTimeoutWatchdog().abort(req.getTimeoutTask());
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
    if (originalMessage != null && originalMessage.requestId() != null && 
        ! originalMessage.isResponse() && originalMessage.from() != null) {
      send(new Message(ref, originalMessage.from(), fault, originalMessage.requestId(), true));
    }
    addToDeadLetterQueue(fault);
    faultReason = null;
    return fault;
  }
  
  private void addToDeadLetterQueue(Fault fault) {
    system.addToDeadLetterQueue(fault);
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
    if (passivationScheduled && state == ACTIVATED && pending.isEmpty() && (stash == null || stash.messages.isEmpty())) {
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
          if (stash != null && ! stash.messages.isEmpty()) {
            stash.messages.remove(0);
          }
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
