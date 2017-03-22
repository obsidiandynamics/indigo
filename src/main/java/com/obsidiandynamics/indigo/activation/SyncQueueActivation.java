package com.obsidiandynamics.indigo.activation;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class SyncQueueActivation extends Activation {
  private static final long PASSIVATION_AWAIT_DELAY = 10;
  
  private boolean activated;
  
  protected boolean passivationScheduled;
  
  protected volatile boolean passivationComplete;
  
  public SyncQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    super(id, ref, system, actorConfig, actor);
  }
  
  @Override
  public void run() {
    final Message[] messages;
    synchronized (backlog) {
      if (message != null) throw new IllegalStateException("Actor " + ref + " was already entered");

      messages = new Message[Math.min(actorConfig.bias, backlog.size())];
      for (int i = 0; i < messages.length; i++) {
        messages[i] = backlog.remove();
      }
      message = messages[0];
    }

    if (! activated) {
      actor.activated(this);
      activated = true;
    }

    for (int i = 0; i < messages.length; i++) {
      message = messages[i];
      processMessage(messages[i]);
    }

    final boolean noBacklog;
    final boolean noPending;
    synchronized (backlog) {
      if (message == null) throw new IllegalStateException("Actor " + ref + " was already cleared");

      message = null;
      noBacklog = backlog.isEmpty();
      noPending = pending.isEmpty();
    }

    if (noBacklog) {
      if (noPending) {
        if (passivationScheduled) {
          system._passivate(ref);
          actor.passivated(this);
          passivationComplete = true;
        }
        system._decBusyActors();
      }
    } else {
      system._dispatch(this);
    }
  }
  
  @Override
  public void enqueue(Message m) throws ActorPassivatedException {
    for (;;) {
      final boolean noBacklog;
      final boolean noPending;
      final boolean awaitPassivation;
      final boolean throttleBacklog;
      synchronized (backlog) {
        noBacklog = message == null && backlog.isEmpty();
        noPending = pending.isEmpty();
        
        awaitPassivation = noBacklog && noPending && passivationScheduled;
        if (! awaitPassivation) {
          throttleBacklog = shouldThrottle();
          if (! throttleBacklog) {
            backlog.add(m);
          }
        } else {
          throttleBacklog = false;
        }
      }
      
      if (throttleBacklog) {
        Threads.throttle(this::shouldThrottle, actorConfig.backlogThrottleTries, actorConfig.backlogThrottleMillis);
        continue;
      }
    
      while (awaitPassivation) {
        if (passivationComplete) {
          throw new ActorPassivatedException();
        } else {
          Threads.sleep(PASSIVATION_AWAIT_DELAY);
        }
      }
      
      if (noBacklog && noPending) {
        system._incBusyActors();
      }
      
      if (noBacklog) {
        system._dispatch(this);
      }
      
      return;
    }
  }
  
  private boolean shouldThrottle() {
    return backlog.size() >= actorConfig.backlogThrottleCapacity;
  }
  
  @Override
  public void passivate() {
    passivationScheduled = true;
  }
}
