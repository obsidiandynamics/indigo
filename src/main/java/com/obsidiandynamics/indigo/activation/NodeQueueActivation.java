package com.obsidiandynamics.indigo.activation;

import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class NodeQueueActivation extends Activation {
  private static final long PASSIVATION_AWAIT_DELAY = 10;
  
  private static final int MAX_SPINS = 10;
  
  private static final class Node extends AtomicReference<Node> {
    private static final long serialVersionUID = 1L;
    
    private final Message m;
    
    Node(Message m) { this.m = m; }
  }
  
  private final AtomicReference<Node> tail = new AtomicReference<>();
  
  private volatile boolean passivationComplete;
  
  /** Raised by the dispatch thread just prior to the CAS parking attempt when passivation is required. If
   *  CAS fails, this flag is immediately lowered. */
  private volatile boolean passivationAttemptStarted;
  
  /** Raised by the dispatch thread just after the CAS parking attempt when passivation is required. If both
   *  the 'attempted' and 'succeeded' flags are true, the queuing threads will back off until passivation
   *  completes. */
  private volatile boolean passivationAttemptSucceeded;
  
  private final AtomicInteger backlogSize;
  
  public NodeQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    super(id, ref, system, actorConfig, actor);
    backlogSize = actorConfig.backlogThrottleCapacity != Integer.MAX_VALUE ? new AtomicInteger() : null;
  }
  
  @Override
  public boolean _enqueue(Message m) {
    if (isPassivating()) {
      while (! passivationComplete) {
        Threads.sleep(PASSIVATION_AWAIT_DELAY);
      }
      return false;
    }
    
    if (shouldThrottle()) {
      Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
    }
    
    if (backlogSize != null) backlogSize.incrementAndGet();
    
    final Node t = new Node(m);
    final Node t1 = tail.getAndSet(t);
    
    if (t1 == null) {
      if (pending.isEmpty()) {
        system._incBusyActors();
      }
      scheduleRun(t);
    } else {
      t1.lazySet(t);
    }
    return true;
  }
  
  private boolean isPassivating() {
    for (;;) {
      if (passivationAttemptStarted) {
        if (passivationAttemptSucceeded) {
          return true;
        }
      } else {
        return false;
      }
    }
  }
  
  private void scheduleRun(Node h) {
    system._dispatch(() -> run(h, false));
  }
  
  private void schedulePark(Node n) {
    system._dispatch(() -> {
      if (! park(n)) {
        run(n, true);
      }
    });
  }
  
  private boolean park(Node n) {
    final boolean noPending = pending.isEmpty();
    if (noPending && passivationScheduled) {
      passivationAttemptStarted = true;
    }
  
    final boolean parked = tail.compareAndSet(n, null);
    if (parked) {
      if (noPending) {
        if (passivationScheduled) {
          passivationAttemptSucceeded = true;
          actor.passivated(this);
          system._passivate(ref);
          passivationComplete = true;
        }
        system._decBusyActors();
      }
    } else if (noPending && passivationScheduled) {
      passivationAttemptStarted = false;
    }
    return parked;
  }
  
  private void run(Node h, boolean skipCurrent) {
    int cycles = 0;
    if (! skipCurrent) {
      cycles++;
      ensureActivated();
      processMessage(h.m);
    }
    
    int spins = 0;
    try {
      for (;;) {
        final Node h1 = h.get();
        if (h1 != null) {
          if (cycles < actorConfig.bias) {
            h = h1;
            cycles++;
            processMessage(h.m);
            spins = 0;
          } else {
            scheduleRun(h1);
            return;
          }
        } else if (spins != MAX_SPINS) {
          spins++;
        } else {
          Thread.yield();
          if (! park(h)) {
            schedulePark(h);
          }
          return;
        }
      }
    } finally {
      if (backlogSize != null) backlogSize.addAndGet(-cycles);
    }
  }
  
  private boolean shouldThrottle() {
    return backlogSize != null && backlogSize.get() >= actorConfig.backlogThrottleCapacity;
  }
}
