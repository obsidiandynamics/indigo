package com.obsidiandynamics.indigo.activation;

import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.benchmark.APActor.*;
import com.obsidiandynamics.indigo.util.*;

public final class NodeQueueActivation extends Activation {
  private static final long PASSIVATION_AWAIT_DELAY = 10;
  
  private static final int MAX_SPINS = 10;
  
  private static final class Node extends AtomicReference<Node> {
    private static final long serialVersionUID = 1L;
    
    private final Message m;
    
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
    if (t1 == SENTINEL_PARKED) {
      system._incBusyActors();
      scheduleRun(t);
    } else if (t1 == SENTINEL_PASSIVATING) {
      //TODO
    } else {
      t1.lazySet(t);
    }
    
    return true;
  }
  
  private void scheduleRun(Node h) {
    system._dispatch(() -> run(h, false));
  }
  
  private void schedulePark(Node n, Node sentinel) {
    system._dispatch(() -> {
      if (! park(n, sentinel)) {
        run(n, true);
      }
    });
  }
  
  private boolean park(Node n, Node sentinel) {
    final boolean parked = tail.compareAndSet(n, sentinel);
    if (parked) {
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
//      actor.act(this);
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
//            actor.act(this);
            spins = 0;
          } else {
            scheduleRun(h1);
            return;
          }
        } else if (spins != MAX_SPINS) {
          spins++;
        } else {
          Thread.yield();
          if (! park(h, SENTINEL_PARKED)) {
            schedulePark(h, SENTINEL_PARKED);
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
