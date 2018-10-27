package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActivationState.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.util.*;

final class NodeQueueActivation extends Activation {
  private static final int MAX_YIELDS = 4;

  private static final byte DISPOSAL_STATE_NEUTRAL = 0;
  private static final byte DISPOSAL_STATE_PROPOSED = 1;
  private static final byte DISPOSAL_STATE_ACCEPTED = 2;
  private static final byte DISPOSAL_STATE_COMPLETE = 3;

  private static final class Node extends AtomicReference<Node> {
    private static final long serialVersionUID = 1L;

    private final Message m;

    Node(Message m) { this.m = m; }
  }

  private final AtomicReference<Node> tail = new AtomicReference<>();
  
  /** Set to by the dispatch thread to {@link #DISPOSAL_STATE_PROPOSED} just prior to the CAS parking
   *  attempt when disposal is required, and subsequently overwritten either to {@link #DISPOSAL_STATE_NEUTRAL}
   *  or {@link #DISPOSAL_STATE_ACCEPTED} depending on whether or not CAS succeeded. Eventually, when
   *  disposal is complete, this field is set to {@link #DISPOSAL_STATE_COMPLETE}.
   */
  private volatile byte disposalState;
  
  private final AtomicInteger backlogSize;

  NodeQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor, Executor executor) {
    super(id, ref, system, actorConfig, actor, executor);
    backlogSize = actorConfig.backlogThrottleCapacity != Integer.MAX_VALUE ? new AtomicInteger() : null;
  }

  @Override
  boolean enqueue(Message m) {
    assert diagnostics().traceMacro("NQA.enqueue: m=%s", m);

    if (! m.isResponse() && shouldThrottle()) {
      assert diagnostics().traceMacro("NQA.enqueue: throttling m=%s, t=%s", m, Thread.currentThread());
      Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
    }

    if (backlogSize != null) backlogSize.incrementAndGet();

    final Node t = new Node(m);
    final Node t1 = tail.getAndSet(t);

    if (isDisposing()) {
      assert diagnostics().traceMacro("NQA.enqueue: awaiting disposal m=%s", m);
      while (disposalState != DISPOSAL_STATE_COMPLETE) {
        Thread.yield();
      }
      return false;
    }

    if (t1 == null) {
      if (pending.isEmpty()) {
        system.incBusyActors();
      }
      assert diagnostics().traceMacro("NQA.enqueue: scheduling m=%s", m);
      scheduleRunStart(t);
    } else {
      t1.lazySet(t);
    }
    return true;
  }

  private boolean isDisposing() {
    for (;;) {
      final byte _disposalState = disposalState;
      if (_disposalState == DISPOSAL_STATE_NEUTRAL) {
        return false;
      } else if (_disposalState == DISPOSAL_STATE_PROPOSED) {
        Thread.yield();
      } else {
        return true;
      }
    }
  }

  private void scheduleRunStart(Node n) {
    dispatch(() -> run(n, false));
  }
  
  private void scheduleRunContinue(Node n) {
    dispatch(() -> run(n, true));
  }

  private boolean park(Node n) {
    assert diagnostics().traceMacro("NQA.park: ref=%s, pending=%d", ref, pending.size());
    final boolean noPending = pending.isEmpty();
    final boolean disposing = getState() == PASSIVATED;
    if (disposing) {
      disposalState = DISPOSAL_STATE_PROPOSED;
    }

    final boolean parked = tail.compareAndSet(n, null);
    if (parked) {
      assert diagnostics().traceMacro("NQA.park: parked ref=%s", ref);
      if (noPending) {
        if (disposing) {
          assert diagnostics().traceMacro("NQA.park: disposed ref=%s", ref);
          disposalState = DISPOSAL_STATE_ACCEPTED;
          system.dispose(ref);
          disposalState = DISPOSAL_STATE_COMPLETE;
        }
        system.decBusyActors();
      }
    } else if (noPending && disposing) {
      disposalState = DISPOSAL_STATE_NEUTRAL;
    }
    return parked;
  }

  private void run(Node h, boolean skipCurrent) {
    assert diagnostics().traceMacro("NQA.run: h.m=%s, skipCurrent=%b", h.m, skipCurrent);

    Node head = h;
    int cycles = 0;
    if (! skipCurrent) {
      cycles++;
      processMessage(head.m);
    }

    final int bias = actorConfig.bias;
    int yields = 0;
    boolean attemptedPark = false;
    try {
      for (;;) {
        final Node h1 = head.get();
        if (h1 != null) {
          if (cycles < bias) {
            head = h1;
            cycles++;
            processMessage(head.m);
            yields = 0;
            attemptedPark = false;
          } else {
            assert diagnostics().traceMacro("NQA.run: scheduling ref=%s", ref);
            scheduleRunStart(h1);
            return;
          }
        } else if (! attemptedPark) {
          passivateIfScheduled();
          if (park(head)) {
            return;
          } else {
            attemptedPark = true;
          }
        } else if (yields != MAX_YIELDS) {
          Thread.yield();
          yields++;
        } else {
          scheduleRunContinue(head);
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