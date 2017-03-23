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
    
    private volatile Node sentinel;
    
    Node(Message m) { this.m = m; }
  }
  
  private static final Node SENTINEL_PARKED = new Node(null);
  private static final Node SENTINEL_PASSIVATING = new Node(null);
  
  private final AtomicReference<Node> tail = new AtomicReference<>(SENTINEL_PARKED);
  
  protected boolean passivationScheduled;
  
  protected volatile boolean passivationComplete;
  
  private final AtomicInteger backlogSize;
  
  public NodeQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    super(id, ref, system, actorConfig, actor);
    backlogSize = actorConfig.backlogThrottleCapacity != Integer.MAX_VALUE ? new AtomicInteger() : null;
  }
  
  @Override
  public boolean _enqueue(Message m) {
    if (shouldThrottle()) {
      Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
    }
    if (backlogSize != null) backlogSize.incrementAndGet();
    
    final Node t = new Node(m);
    final Node t1 = tail.getAndSet(t);
    
    for (;;) {
      if (t1 == SENTINEL_PARKED) {
        t.sentinel = t1;
        if (pending.isEmpty()) {
          system._incBusyActors();
        }
        scheduleRun(t);
        return true;
      } else if (t1 == SENTINEL_PASSIVATING) {
        t.sentinel = t1;
        if (passivationComplete) {
          return false;
        } else {
          Threads.sleep(PASSIVATION_AWAIT_DELAY);
        }
      } else {
        if (t1.sentinel == SENTINEL_PASSIVATING) {
          t.sentinel = t1.sentinel;
          if (passivationComplete) {
            return false;
          } else {
            Threads.sleep(PASSIVATION_AWAIT_DELAY);
          }
        } else if (t1.sentinel == SENTINEL_PARKED) {
          t.sentinel = t1.sentinel;
          t1.lazySet(t);
          return true;
        } else {
          Thread.yield();
        }
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
    final Node sentinel = passivationScheduled ? SENTINEL_PASSIVATING : SENTINEL_PARKED;
    final boolean parked = tail.compareAndSet(n, sentinel);
    if (parked && pending.isEmpty()) {
      if (passivationScheduled) {
        actor.passivated(this);
        system._passivate(ref);
        passivationComplete = true;
      }
      system._decBusyActors();
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
      while (true) {
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
  
  @Override
  public void passivate() {
    passivationScheduled = true;
  }
}
