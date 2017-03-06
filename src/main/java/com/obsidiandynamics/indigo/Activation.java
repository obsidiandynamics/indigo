package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

public final class Activation {
  private final ActorRef ref;
  
  private final ActorSystem system;
  
  private final ActorConfig actorConfig;
  
  private final Actor actor;
  
  private final Deque<Message> backlog = new LinkedList<>();
  
  private final Map<UUID, PendingRequest> pending = new HashMap<>();
  
  private Message message;
  
  private boolean activated;
  
  private boolean passivationScheduled;
  
  Activation(ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    this.ref = ref;
    this.system = system;
    this.actorConfig = actorConfig;
    this.actor = actor;
  }
  
  void run() {
    int runsRemaining = actorConfig.priority;
    do {
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
        final PendingRequest req = pending.remove(message.requestId());
        
        if (message.body() instanceof Signal) {
          if (message.body() instanceof TimeoutSignal) {
            if (req != null && ! req.isComplete()) {
              req.setComplete(true);
              req.getOnTimeout().accept(this);
            }
          } else {
            throw new UnsupportedOperationException("Unsupported signal of type " + message.body().getClass().getName());
          }
        } else {
          if (req != null) {
            req.setComplete(true);
            req.getOnResponse().accept(this);
          }
        }
      } else {
        actor.act(this);
      }
  
      final boolean noBacklog;
      final boolean noPending;
      synchronized (backlog) {
        if (message == null) throw new IllegalStateException("Actor " + ref + " was already cleared");
        
        message = null;
        noBacklog = backlog.isEmpty();
        noPending = pending.isEmpty();
      }
      
      runsRemaining--;
      if (! noBacklog) {
        if (runsRemaining == 0) {
          system.dispatch(this);
        }
      } else if (noPending) {
        runsRemaining = 0;
        if (passivationScheduled) {
          system.passivate(ref);
          actor.passivated(this);
        }
        system.decBusyActors();
      }
      system.decBacklog();
    } while (runsRemaining > 0);
  }
  
  void enqueue(Message m) throws ActorPassivatingException {
    final boolean noBacklog;
    final boolean noPending;
    synchronized (backlog) {
      noBacklog = message == null && backlog.isEmpty();
      noPending = pending.isEmpty();
      
      if (noBacklog && noPending && passivationScheduled) throw new ActorPassivatingException();
      
      backlog.addLast(m);
      
      if (noBacklog && noPending) {
        system.incBusyActors();
      }
      system.incBacklog();
    }
    
    if (noBacklog) {
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
    
    private int copies = 1;
    
    private Object requestBody;
    
    private long timeoutMillis;
    
    private Consumer<Activation> onTimeout;
    
    MessageBuilder(ActorRef to) {
      this.to = to;
    }
    
    public MessageBuilder times(int copies) {
      this.copies = copies;
      return this;
    }
    
    public void tell(Object body) {
      for (int i = copies; --i >= 0;) {
        send(new Message(ref, to, body, null, false));
      }
    }
    
    public MessageBuilder ask(Object requestBody) {
      if (copies != 1) throw new IllegalArgumentException("Cannot 'ask' with more than 1 copy");
      this.requestBody = requestBody;
      return this;
    }
    
    public MessageBuilder ask() {
      return ask(null);
    }
    
    public MessageBuilder await(long timeoutMillis) {
      this.timeoutMillis = timeoutMillis;
      return this;
    }
    
    public MessageBuilder onTimeout(Consumer<Activation> onTimeout) {
      this.onTimeout = onTimeout;
      return this;
    }
    
    public void onResponse(Consumer<Activation> onResponse) {
      if (timeoutMillis != 0 ^ onTimeout != null) {
        throw new IllegalArgumentException("Only one of the timeout time or handler has been set");
      }      
      
      final UUID requestId = UUID.randomUUID();
      final PendingRequest req = new PendingRequest(onResponse, onTimeout);
      pending.put(requestId, req);
      send(new Message(ref, to, requestBody, requestId, false));
      
      if (timeoutMillis != 0) {
        system.getTimeoutWatchdog().enqueue(new TimeoutTask(System.nanoTime() + timeoutMillis * 1_000_000l,
                                                            requestId,
                                                            Activation.this,
                                                            req));
      }
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
  
  public void reply() {
    reply(null);
  }
  
  public void reply(Object responseBody) {
    final boolean responding = message.requestId() != null;
    send(new Message(ref, message.from(), responseBody, message.requestId(), responding));
  }
  
  public void forward(ActorRef to) {
    send(new Message(message.from(), to, message.body(), message.requestId(), message.isResponse()));
  }
  
  private void send(Message message) {
    system.send(message, actorConfig.throttleSend);
  }

  @Override
  public String toString() {
    return "Activation [ref=" + ref + ", message=" + message + "]";
  }
}
