package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public final class Activation {
  private final ActorRef ref;
  
  private final ActorSystem system;
  
  private final Actor actor;
  
  private final Deque<Message> backlog = new LinkedList<>();
  
  private final Map<UUID, PendingRequest> requests = new HashMap<>();
  
  private Message message;
  
  private boolean activated;
  
  private boolean passivationScheduled;
  
  Activation(ActorRef ref, ActorSystem system, Actor actor) {
    this.ref = ref;
    this.system = system;
    this.actor = actor;
  }
  
  AtomicInteger entries = new AtomicInteger();
  void run() {
    entries.incrementAndGet();
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
    
    if (message.isResponse()) {
      final PendingRequest req = requests.remove(message.requestId());
      if (req == null) {
        throw new IllegalStateException("No pending request for ID " + message.requestId());
      }
      req.getOnResponse().accept(this);
    } else {
      actor.act(this);
    }

    final boolean backlogEmpty;
    synchronized (backlog) {
      if (message == null) throw new IllegalStateException("Actor " + ref + " was already cleared");
      
      message = null;
      backlogEmpty = backlog.isEmpty();
    }
    
    if (! backlogEmpty) {
      system.dispatch(this);
    } else { 
      system.decBusyActors();
      if (passivationScheduled) {
        system.passivate(ref);
        actor.passivated(this);
      }
    }
    system.decBacklog();
  }
  
  void enqueue(Message m) throws ActorPassivatingException {
    final boolean wasEmpty;
    synchronized (backlog) {
      wasEmpty = message == null && backlog.isEmpty();
      if (wasEmpty && passivationScheduled) throw new ActorPassivatingException();
      
      backlog.addLast(m);
      
      if (wasEmpty) {
        system.incBusyActors();
      }
      system.incBacklog();
    }
    
    if (wasEmpty) {
      system.dispatch(this);
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
    
    private Object requestBody;
    
    private int timeoutMillis;
    
    private Consumer<Activation> onTimeout;
    
    MessageBuilder(ActorRef to) {
      this.to = to;
    }
    
    public void tell(Object body) {
      system.send(new Message(ref, to, body, null, false));
    }
    
    public MessageBuilder ask(Object requestBody) {
      this.requestBody = requestBody;
      return this;
    }
    
    public MessageBuilder ask() {
      return ask(null);
    }
    
    public MessageBuilder after(int timeoutMillis) {
      this.timeoutMillis = timeoutMillis;
      return this;
    }
    
    public MessageBuilder timeout(Consumer<Activation> onTimeout) {
      this.onTimeout = onTimeout;
      return this;
    }
    
    public void response(Consumer<Activation> onResponse) {
      if (timeoutMillis != 0 ^ onTimeout != null) {
        throw new IllegalStateException("Only one of the timeout time or handler has been set");
      }
      final UUID requestId = UUID.randomUUID();
      requests.put(requestId, new PendingRequest(onResponse, onTimeout));
      system.send(new Message(ref, to, requestBody, requestId, false));
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
  
  public void reply(Object responseBody) {
    system.send(new Message(ref, message.from(), responseBody, message.requestId(), true));
  }

  @Override
  public String toString() {
    return "Activation [ref=" + ref + ", message=" + message + "]";
  }
}
