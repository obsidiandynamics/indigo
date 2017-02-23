package com.obsidiandynamics.indigo;

import java.util.*;

final class Activation {
  private final ActorId id;
  
  private final ActorSystem system;
  
  private final Actor actor;
  
  private final Deque<Message> backlog = new LinkedList<>();
  
  private boolean busy;

  Activation(ActorId id, ActorSystem system, Actor actor) {
    this.id = id;
    this.system = system;
    this.actor = actor;
  }
  
  void run() {
    final Message m;
    synchronized (backlog) {
      if (busy) throw new IllegalStateException("Actor " + id + " was already entered");
      
      busy = true;
      m = backlog.removeFirst();
    }
    
    actor.act(m);
    
    synchronized (backlog) {
      if (! busy) throw new IllegalStateException("Actor " + id + " was already cleared");
      
      busy = false;
      if (! backlog.isEmpty()) {
        system.dispatch(this);
      }
    }
  }
  
  void enqueue(Message m) {
    synchronized (backlog) {
      final boolean wasEmpty = backlog.isEmpty();
      backlog.addLast(m);
      if (wasEmpty) {
        system.dispatch(this);
      }
    }
  }
}
