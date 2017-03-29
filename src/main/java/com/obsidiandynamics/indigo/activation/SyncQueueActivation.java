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
  public boolean _enqueue(Message m) {
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
        
        throttleBacklog = shouldThrottle();
        if (! throttleBacklog) {
          backlog.add(m);
        }
      }
      
      if (throttleBacklog) {
        Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
        continue;
      }
    
      if (noBacklog && noPending) {
        system._incBusyActors();
      }
      
      if (noBacklog) {
        system._dispatch(this::run);
      }
      
      return true;
    }
  }
  
  private void run() {
    final Message[] messages;
    synchronized (backlog) {
      if (on) throw new IllegalStateException("Actor " + ref + " was already entered");

      messages = new Message[Math.min(actorConfig.bias, backlog.size())];
      for (int i = 0; i < messages.length; i++) {
        messages[i] = backlog.remove();
      }
      on = true;
    }

    ensureActivated();

    for (int i = 0; i < messages.length; i++) {
      processMessage(messages[i]);
    }
    
    passivateIfScheduled();

    final boolean noBacklog;
    final boolean noPending;
    synchronized (backlog) {
      if (! on) throw new IllegalStateException("Actor " + ref + " was already cleared");

      on = false;
      noBacklog = backlog.isEmpty();
      noPending = pending.isEmpty();
      
      if (state == PASSIVATED) {
        system._dispose(ref);
        disposed = true;
      }
    }

    if (noBacklog) {
      if (noPending) {
        system._decBusyActors();
      }
    } else {
      system._dispatch(this::run);
    }
  }
  
  private boolean shouldThrottle() {
    return backlog.size() >= actorConfig.backlogThrottleCapacity;
  }
}
