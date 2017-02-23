package com.obsidiandynamics.indigo;

import java.util.*;

final class Activation {
  private final ActorId id;
  
  private final ActorSystem system;
  
  private final Actor actor;
  
  private final Deque<Message> backlog = new LinkedList<>();
  
  private Message message;

  Activation(ActorId id, ActorSystem system, Actor actor) {
    this.id = id;
    this.system = system;
    this.actor = actor;
  }
  
  void run() {
    synchronized (backlog) {
      if (message != null) throw new IllegalStateException("Actor " + id + " was already entered");
      
      message = backlog.removeFirst();
    }
    
    actor.act(this);
    
    synchronized (backlog) {
      if (message == null) throw new IllegalStateException("Actor " + id + " was already cleared");
      
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
  
  public ActorId id() {
    return id;
  }
  
  public Message message() {
    return message;
  }
  
  public final class MessageBuilder {
    private final ActorId to;

    MessageBuilder(ActorId to) {
      this.to = to;
    }
    
    public void tell(Object body) {
      system.send(new Message(id, to, body));
    }
  }
  
  public MessageBuilder to(ActorId to) {
    return new MessageBuilder(to);
  }
  
  public MessageBuilder self() {
    return to(id);
  }

  @Override
  public String toString() {
    return "Activation [id=" + id + ", message=" + message + "]";
  }
}
