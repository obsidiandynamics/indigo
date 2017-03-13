package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class Activation {
  private final long id;
  
  private final ActorRef ref;
  
  private final ActorSystem system;
  
  private final ActorConfig actorConfig;
  
  private final Actor actor;
  
  private final Queue<Message> backlog = new ArrayDeque<>(1);
  
  private final Map<UUID, PendingRequest> pending = new HashMap<>();
  
  private Message message;
  
  private boolean activated;
  
  private boolean passivationScheduled;
  
  private long requestCounter = CryptoUtils.machineRandom();
  
  Activation(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor) {
    this.id = id;
    this.ref = ref;
    this.system = system;
    this.actorConfig = actorConfig;
    this.actor = actor;
  }
  
  void run() {
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
        } else if (req != null) {
          if (req.getTimeoutTask() != null) {
            system.getTimeoutWatchdog().dequeue(req.getTimeoutTask());
          }
          req.setComplete(true);
          req.getOnResponse().accept(this);
        }
      } else {
        actor.act(this);
      }
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
          system.passivate(ref);
          actor.passivated(this);
        }
        system.decBusyActors();
      }
    } else {
      system.dispatch(this);
    }
    system.decBacklog();
  }
  
  void enqueue(Message m) throws ActorPassivatingException {
    final boolean noBacklog;
    final boolean noPending;
    synchronized (backlog) {
      noBacklog = message == null && backlog.isEmpty();
      noPending = pending.isEmpty();
      
      if (noBacklog && noPending && passivationScheduled) throw new ActorPassivatingException();
      
      backlog.add(m);
    }
    
    if (noBacklog && noPending) {
      system.incBusyActors();
    }
    system.incBacklog();
    
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
  
  @FunctionalInterface
  private interface MessageTarget {
    void send(Object body, UUID requestId);
  }
  
  public final class MessageBuilder {
    private final MessageTarget target;
    
    private int copies = 1;
    
    private Object requestBody;
    
    private long timeoutMillis;
    
    private Consumer<Activation> onTimeout;
    
    MessageBuilder(MessageTarget target) {
      this.target = target;
    }
    
    public MessageBuilder times(int copies) {
      this.copies = copies;
      return this;
    }
    
    public void tell(Object body) {
      for (int i = copies; --i >= 0;) {
        target.send(body, null);
      }
    }
    
    public MessageBuilder ask(Object requestBody) {
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

      for (int i = copies; --i >= 0;) {
        final UUID requestId = new UUID(id, requestCounter++);
        final PendingRequest req = new PendingRequest(onResponse, onTimeout);
        pending.put(requestId, req);
        target.send(requestBody, requestId);
        
        if (timeoutMillis != 0) {
          final TimeoutTask timeoutTask = new TimeoutTask(System.nanoTime() + timeoutMillis * 1_000_000l,
                                                          requestId,
                                                          Activation.this,
                                                          req);
          req.setTimeoutTask(timeoutTask);
          system.getTimeoutWatchdog().enqueue(timeoutTask);
        }
      }
    }
    
    public void tell() {
      tell(null);
    }
  }
  
  public MessageBuilder to(ActorRef to) {
    return new MessageBuilder((body, requestId) -> send(new Message(ref, to, body, requestId, false)));
  }
  
  public final class EgressBuilder<I, O> {
    private final Function<I, O> func;

    EgressBuilder(Function<I, O> func) {
      this.func = func;
    }

    @SuppressWarnings("unchecked")
    public MessageBuilder using(Executor executor) {
      return new MessageBuilder((body, requestId) -> {
        executor.execute(() -> {
          final O out = func.apply((I) body);
          if (requestId != null) {
            final Message resp = new Message(null, ref, out, requestId, true);
            system.send(resp, false);
          }
        });
      });
    }
  }
  
  public <I, O> EgressBuilder<I, O> egress(Function<I, O> func) {
    return new EgressBuilder<>(func);
  }
  
  public <I> EgressBuilder<I, Void> egress(Consumer<I> consumer) {
    return new EgressBuilder<>(in -> {
      consumer.accept(in);
      return null;
    });
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
