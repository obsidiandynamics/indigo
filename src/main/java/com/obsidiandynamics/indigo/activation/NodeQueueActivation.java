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

  @Override
  public boolean enqueue(Message m) {
    final Diagnostics d = diagnostics();
    if (d.traceEnabled) d.trace("NQA.enqueue: m=%s", m);

    if (! m.isResponse() && shouldThrottle()) {
      if (d.traceEnabled) d.trace("NQA.enqueue: throttling m=%s, t=%s", m, Thread.currentThread());
      Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
    }

    if (backlogSize != null) backlogSize.incrementAndGet();

    final Node t = new Node(m);
    final Node t1 = tail.getAndSet(t);

    if (isDisposing()) {
      if (d.traceEnabled) d.trace("NQA.enqueue: awaiting disposal m=%s", m);
      while (! disposalComplete) {
        Thread.yield();
      }
      if (backlogSize != null) backlogSize.incrementAndGet();
      return false;
    }

    if (t1 == null) {
      if (pending.isEmpty()) {
        system._incBusyActors();
      }

      if (isDisposing()) {
        if (d.traceEnabled) d.trace("NQA.enqueue: awaiting disposal m=%s", m);
        while (! disposalComplete) {
          Thread.yield();
        }
        return false;
      }
      if (d.traceEnabled) d.trace("NQA.enqueue: scheduling m=%s", m);
      scheduleRun(t);
    } else {
      t1.lazySet(t);
    }
    return true;
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
    if (d.traceEnabled) d.trace("NQA.park: ref=%s, pending=%d", ref, pending.size());
    final boolean noPending = pending.isEmpty();
    final boolean disposing = state == PASSIVATED;
    if (disposing) {
      disposalAttemptProposed = true;
    }

    final boolean parked = tail.compareAndSet(n, null);
    if (parked) {
      if (d.traceEnabled) d.trace("NQA.park: parked ref=%s", ref);
      if (noPending) {
        if (disposing) {
          if (d.traceEnabled) d.trace("NQA.park: disposed ref=%s", ref);
          disposalAttemptAccepted = true;
          system._dispose(ref);
          disposalComplete = true;
        }
        system._decBusyActors();
      }
    } else if (noPending && disposing) {
      disposalAttemptProposed = false;
    }
    return parked;
  }

  private void run(Node h, boolean skipCurrent) {
    final Diagnostics d = diagnostics();
    if (d.traceEnabled) d.trace("NQA.run: h.m=%s, skipCurrent=%b", h.m, skipCurrent);

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
            if (d.traceEnabled) d.trace("NQA.run: scheduling ref=%s", ref);
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
  }

  private boolean shouldThrottle() {
    return backlogSize != null && backlogSize.get() >= actorConfig.backlogThrottleCapacity;
  }
}
