package com.obsidiandynamics.indigo;

import java.util.*;

public final class Activation {
  private final ActorRef ref;
  
  private final ActorSystem system;
  
  private final Actor actor;
  
  private final Deque<Message> backlog = new LinkedList<>();
  
  private Message message;
  
  private boolean activated;
  
  private boolean passivationScheduled;
  
  Activation(ActorRef ref, ActorSystem system, Actor actor) {
    this.ref = ref;
    this.system = system;
    this.actor = actor;
  }
  
  void run() {
    final boolean activationRequired;
    synchronized (backlog) {
      if (message != null) throw new IllegalStateException("Actor " + ref + " was already entered");
      
      message = backlog.removeFirst();
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
    
    actor.act(this);
    
    synchronized (backlog) {
      if (message == null) throw new IllegalStateException("Actor " + ref + " was already cleared");
      
      message = null;
      if (! backlog.isEmpty()) {
        system.dispatch(this);
      } else {
        system.decBusyActors();
        if (passivationScheduled) {
          system.passivate(ref);
          actor.passivated(this);
        }
      }
    }
  }
  
  void enqueue(Message m) throws ActorPassivatingException {
    synchronized (backlog) {
      final boolean wasEmpty = message == null && backlog.isEmpty();
      if (wasEmpty && passivationScheduled) throw new ActorPassivatingException();
      
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
  
  public void passivate() {
    passivationScheduled = true;
  }
  
  public final class MessageBuilder {
    private final ActorRef to;

    MessageBuilder(ActorRef to) {
      this.to = to;
    }
    
    public void tell(Object body) {
      system.send(new Message(ref, to, body));
    }
    
    public void tell() {
      tell(null);
    }
  }
  
  public MessageBuilder to(ActorRef to) {
    return new MessageBuilder(to);
  }
  
  public MessageBuilder toSelf() {
    return to(ref);
  }
  
  public MessageBuilder toSender() {
    return to(message.from());
  }

  @Override
  public String toString() {
    return "Activation [ref=" + ref + ", message=" + message + "]";
  }
}
