package com.obsidiandynamics.indigo.activation;

import static com.obsidiandynamics.indigo.ActivationState.*;

import java.util.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class SyncQueueActivation extends Activation {
  private final Queue<Message> backlog = new ArrayDeque<>(1);
  
  private boolean on;
  
  private boolean disposed;
  
  public SyncQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    super(id, ref, system, actorConfig, actor);
  }
  
  @Override
  public boolean enqueue(Message m) {
    assert diagnostics().traceMacro("SQA.enqueue: m=%s", m);
    
    boolean throttledOnce = false;
    for (;;) {
      final boolean noBacklog;
      final boolean noPending;
      final boolean throttleBacklog;
      synchronized (backlog) {
        if (disposed) {
          return false;
        }
        
        noBacklog = ! on && backlog.isEmpty();
        noPending = pending.isEmpty();
        
        throttleBacklog = ! throttledOnce && ! m.isResponse() && shouldThrottle();
        if (! throttleBacklog) {
          backlog.add(m);
        }
      }
      
      if (throttleBacklog) {
        assert diagnostics().traceMacro("SQA.enqueue: throttling m=%s", m);
        throttledOnce = true;
        Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
        continue;
      }
    
      if (noBacklog && noPending) {
        system._incBusyActors();
      }
      
      if (noBacklog) {
        assert diagnostics().traceMacro("SQA.enqueue: scheduling m=%s", m);
        system._dispatch(this::run);
      }
      
      return true;
    }
  }
  
  private void run() {
    assert diagnostics().traceMacro("SQA.run: ref=%s", ref);
    final Message[] messages;
    final int backlogSize;
    synchronized (backlog) {
      if (on) throw new FrameworkError("Actor " + ref + " was already entered");

      backlogSize = backlog.size();
      messages = new Message[Math.min(actorConfig.bias, backlogSize)];
      for (int i = 0; i < messages.length; i++) {
        messages[i] = backlog.remove();
      }
      on = true;
    }

    int processed = 0;
    for (; processed < messages.length; processed++) {
      processMessage(messages[processed]);
    }
    
    if (processed == backlogSize) {
      passivateIfScheduled();
    }

    final boolean noBacklog;
    final boolean noPending;
    synchronized (backlog) {
      if (! on) throw new FrameworkError("Actor " + ref + " was already cleared");

      on = false;
      noBacklog = backlog.isEmpty();
      noPending = pending.isEmpty();
      
      if (noBacklog && getState() == PASSIVATED) {
        assert diagnostics().traceMacro("SQA.run: disposing ref=%s", ref);
        system._dispose(ref);
        disposed = true;
      }
    }

    if (noBacklog) {
      if (noPending) {
        system._decBusyActors();
      }
    } else {
      assert diagnostics().traceMacro("SQA.run: scheduling, ref=%s", ref);
      system._dispatch(this::run);
    }
  }
  
  private boolean shouldThrottle() {
    synchronized (backlog) {
      return backlog.size() >= actorConfig.backlogThrottleCapacity;
    }
  }
}
