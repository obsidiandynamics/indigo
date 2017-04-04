package com.obsidiandynamics.indigo.activation;

import static com.obsidiandynamics.indigo.ActivationState.*;

import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class NodeQueueActivation extends Activation {
  private static final int MAX_SPINS = 10;
  
  private static final class Node extends AtomicReference<Node> {
    private static final long serialVersionUID = 1L;
    
    private final Message m;
    
    Node(Message m) { this.m = m; }
  }
  
  private final AtomicReference<Node> tail = new AtomicReference<>();
  
  private volatile boolean disposalComplete;
  
  /** Raised by the dispatch thread just prior to the CAS parking attempt when disposal is required. If
   *  CAS fails, this flag is immediately lowered. */
  private volatile boolean disposalAttemptProposed;
  
  /** Raised by the dispatch thread just after the CAS parking attempt when disposal is required. If both
   *  the 'attempted' and 'succeeded' flags are true, the queuing threads will back off until disposal
   *  completes. */
  private volatile boolean disposalAttemptAccepted;
  
  private final AtomicInteger backlogSize;
  
  public NodeQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    super(id, ref, system, actorConfig, actor);
    backlogSize = actorConfig.backlogThrottleCapacity != Integer.MAX_VALUE ? new AtomicInteger() : null;
  }
  
  int busyCount; //TODO
  
  @Override
  public boolean enqueue(Message m) {
    try {
      final Diagnostics d = diagnostics();
      final boolean traceEnabled = d.traceEnabled;
      if (traceEnabled) d.trace("enqueuing m=%s", m);
      
      if (! m.isResponse() && shouldThrottle()) {
        if (traceEnabled) d.trace("throttling m=%s, t=%s", m, Thread.currentThread());
        Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
      }
      
      if (backlogSize != null) backlogSize.incrementAndGet();
      
      final Node t = new Node(m);
      final Node t1 = tail.getAndSet(t);
      
      if (isDisposing()) {
        if (traceEnabled) d.trace("awaiting disposal m=%s", m);
        while (! disposalComplete) {
          Thread.yield();
        }
        if (backlogSize != null) backlogSize.incrementAndGet();
        return false;
      }
      
      if (t1 == null) {
        if (pending.isEmpty()) {
          busyCount++;
          system._incBusyActors();
        }
        
        if (isDisposing()) {
          if (traceEnabled) d.trace("awaiting disposal m=%s", m);
          while (! disposalComplete) {
            Thread.yield();
          }
          return false;
        }
        if (traceEnabled) d.trace("scheduling m=%s", m);
        scheduleRun(t);
      } else {
        t1.lazySet(t);
      }
      return true;
    } catch (RuntimeException e) {
      System.err.println("Exception in NQA.enqueue()");
      e.printStackTrace();
      throw e;
    }
  }
  
  private boolean isDisposing() {
    for (;;) {
      if (disposalAttemptProposed) {
        if (disposalAttemptAccepted) {
          return true;
        }
        Thread.yield();
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
    final Diagnostics d = diagnostics();
    if (d.traceEnabled) d.trace("parking ref=%s, pending=%d", ref, pending.size());
    final boolean noPending = pending.isEmpty();
    final boolean disposing = state == PASSIVATED;
    if (disposing) {
      disposalAttemptProposed = true;
    }
  
    final boolean parked = tail.compareAndSet(n, null);
    if (parked) {
      if (d.traceEnabled) d.trace("parked ref=%s, busyCount=%d", ref, busyCount);
      if (noPending) {
        if (disposing) {
          if (d.traceEnabled) d.trace("disposed ref=%s", ref);
          disposalAttemptAccepted = true;
          system._dispose(ref);
          disposalComplete = true;
        }
        busyCount--;
        system._decBusyActors();
      }
    } else if (noPending && disposing) {
      disposalAttemptProposed = false;
    }
    return parked;
  }
  
  private void run(Node h, boolean skipCurrent) {
    try {
      final Diagnostics d = diagnostics();
      if (d.traceEnabled) d.trace("run h.m=%s, skipCurrent=%b", h.m, skipCurrent);
        
      int cycles = 0;
      if (! skipCurrent) {
        cycles++;
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
            passivateIfScheduled();
            if (! park(h)) {
              schedulePark(h);
            }
            return;
          }
        }
      } finally {
        if (backlogSize != null) backlogSize.addAndGet(-cycles);
      }
    } catch (Throwable e) {
      System.err.println("Exception in NQA.run()");
      e.printStackTrace();
    }
  }
  
  private boolean shouldThrottle() {
    return backlogSize != null && backlogSize.get() >= actorConfig.backlogThrottleCapacity;
  }
}
