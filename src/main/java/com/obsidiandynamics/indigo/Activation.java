package com.obsidiandynamics.indigo;

import java.util.*;

final class Activation {
  private final ActorRef ref;
  
  private final ActorSystem system;
  
  private final Actor actor;
  
  private final Deque<Message> backlog = new LinkedList<>();
  
  private Message message;

  Activation(ActorRef ref, ActorSystem system, Actor actor) {
    this.ref = ref;
    this.system = system;
    this.actor = actor;
  }
  
  void run() {
    synchronized (backlog) {
      if (message != null) throw new IllegalStateException("Actor " + ref + " was already entered");
      
      message = backlog.removeFirst();
    }
    
    actor.act(this);
    
    synchronized (backlog) {
      if (message == null) throw new IllegalStateException("Actor " + ref + " was already cleared");
      
      message = null;
      if (! backlog.isEmpty()) {
        system.dispatch(this);
      } else {
        system.decBusyActors();
      }
    }
  }
  
  void enqueue(Message m) {
    synchronized (backlog) {
      final boolean wasEmpty = message == null && backlog.isEmpty();
      backlog.addLast(m);
      if (wasEmpty) {
        system.dispatch(this);
        system.incBusyActors();
      }
    }
  }
  
  public ActorRef self() {
    return ref;
  }
  
  public Message message() {
    return message;
  }
  
  public final class MessageBuilder {
    private final ActorRef to;

    MessageBuilder(ActorRef to) {
      this.to = to;
    }
    
    public void tell(Object body) {
      system.send(new Message(ref, to, body));
    }
  }
  
  public MessageBuilder to(ActorRef to) {
    return new MessageBuilder(to);
  }
  
  public MessageBuilder toSelf() {
    return to(ref);
  }

  @Override
  public String toString() {
    return "Activation [ref=" + ref + ", message=" + message + "]";
  }
}
