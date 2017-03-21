package com.obsidiandynamics.indigo.activation;

import com.obsidiandynamics.indigo.*;

public final class SyncQueueActivation extends Activation {
  private boolean activated;
  
  public SyncQueueActivation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    super(id, ref, system, actorConfig, actor);
  }
  
  @Override
  public void run() {
    final boolean activationRequired;
    
    final Message[] messages;
    synchronized (backlog) {
      if (message != null) throw new IllegalStateException("Actor " + ref + " was already entered");

      messages = new Message[Math.min(actorConfig.priority, backlog.size())];
      for (int i = 0; i < messages.length; i++) {
        messages[i] = backlog.remove();
      }
      message = messages[0];
      
      if (activated == false) {
        activationRequired = true;
        activated = true;
      } else {
        activationRequired = false;
      }
    }

    if (activationRequired) {
      actor.activated(this);
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
        }
        system._decBusyActors();
      }
    } else {
      system._dispatch(this);
    }
    system._adjBacklog(-messages.length);
  }
  
  @Override
  public void enqueue(Message m) throws ActorPassivatingException {
    final boolean noBacklog;
    final boolean noPending;
    synchronized (backlog) {
      noBacklog = message == null && backlog.isEmpty();
      noPending = pending.isEmpty();
      
      if (noBacklog && noPending && passivationScheduled) throw new ActorPassivatingException();
      
      backlog.add(m);
    }
    
    if (noBacklog && noPending) {
      system._incBusyActors();
    }
    system._adjBacklog(1);
    
    if (noBacklog) {
      system._dispatch(this);
    }
  }
}
