package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public final class ActorSystem {
  private final ExecutorService executor;
  
  private final Map<ActorRef, Activation> activations = new ConcurrentHashMap<>();
  
  private final Map<Object, Supplier<Actor>> factories = new HashMap<>();
  
  private final AtomicInteger busyActors = new AtomicInteger();
  
  private final AtomicLong backlog = new AtomicLong();
  
  private final ActorRef ingressRef = ActorRef.of("_ingress");
  
  private final Activation ingressActivation;
  
  private final long backlogCapacity = 100_000;
  private final int backlogBackoff = 10;
  
  ActorSystem(ActorSystemConfig config) {
    executor = Executors.newFixedThreadPool(config.numThreads);
    when(ingressRef.role()).lambda(a -> {});
    ingressActivation = activate(ingressRef);
  }
  
  public ActorSystem ingress(Consumer<Activation> act) {
    act.accept(ingressActivation);
    return this;
  }
  
  public final class ActorBuilder {
    private final Object role;
    
    ActorBuilder(Object role) { 
      this.role = role;
    }
    
    public ActorSystem lambda(Consumer<Activation> act) {
      return use(StatelessLambdaActor.builder().act(act)); 
    }
    
    public <S> ActorSystem lambda(Supplier<S> stateFactory, BiConsumer<Activation, S> act) {
      return use(StatefulLambdaActor.<S>builder().act(act).activated(a -> stateFactory.get()));
    }
    
    public ActorSystem use(Supplier<Actor> factory) {
      register(role, factory);
      return ActorSystem.this;
    }
  }
  
  public ActorBuilder when(Object role) {
    return new ActorBuilder(role);
  }
  
  private void register(Object role, Supplier<Actor> factory) {
    final Supplier<Actor> existing = factories.put(role, factory);
    if (existing != null) {
      factories.put(role, existing);
      throw new IllegalStateException("Factory for actor of role " + role + " has already been registered");
    }
  }
  
  ActorSystem send(Message m) {
    throttleBacklog(m.from());
    
    while (true) {
      final Activation a = activate(m.to());
      try {
        a.enqueue(m);
        break;
      } catch (ActorPassivatingException e) {}
    }
    return this;
  }
  
  private void throttleBacklog(ActorRef from) {
    while (from == ingressRef && backlog.get() > backlogCapacity) {
      try {
        Thread.sleep(backlogBackoff);
      } catch (InterruptedException e) {}
    }
  }
  
  void dispatch(Activation a) {
    executor.execute(new DispatchTask(a));
  }
  
  void incBusyActors() {
    busyActors.incrementAndGet();
  }
  
  void decBusyActors() {
    final int newCount = busyActors.decrementAndGet();
    if (newCount == 0) {
      synchronized (busyActors) {
        busyActors.notifyAll();
      }
    }
  }
  
  void incBacklog() {
    backlog.incrementAndGet();
  }
  
  void decBacklog() {
    backlog.decrementAndGet();
  }
  
  public ActorSystem await() throws InterruptedException {
    while (busyActors.get() != 0) {
      synchronized (busyActors) {
        busyActors.wait(1_000);
      }
    }
    return this;
  }
  
  private Activation activate(ActorRef ref) {
    final Activation existing = activations.get(ref);
    if (existing != null) {
      return existing;
    } else {
      synchronized (activations) {
        final Activation existing2 = activations.get(ref);
        if (existing2 != null) {
          return existing2;
        } else {
          final Activation created = new Activation(ref, this, createActor(ref.role()));
          activations.put(ref, created);
          return created;
        }
      }
    }
  }
  
  void passivate(ActorRef ref) {
    activations.remove(ref);
  }
  
  private Actor createActor(Object role) {
    final Supplier<Actor> factory = factories.get(role);
    if (factory == null) throw new IllegalArgumentException("No registered factory for actor of role " + role);
    return factory.get();
  }

  public void shutdown() {
    while (true) {
      try {
        await();
        break;
      } catch (InterruptedException e) {}
    }
    executor.shutdown();
  }
}
