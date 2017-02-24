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
  
  private final ActorRef rootId = ActorRef.of("_root");
  
  private final Activation rootActivation;
  
  public ActorSystem() {
    this(getDefaultThreads());
  }
  
  public ActorSystem(int numThreads) {
    executor = Executors.newWorkStealingPool(numThreads);
    register(rootId.role(), () -> new StatelessLambdaActor(a -> {}));
    rootActivation = activate(rootId);
  }
  
  public ActorSystem ingress(Consumer<Activation> consumer) {
    consumer.accept(rootActivation);
    return this;
  }
  
  public final class ActorBuilder {
    private final Object role;
    
    ActorBuilder(Object role) { 
      this.role = role;
    }
    
    public ActorSystem apply(Consumer<Activation> consumer) {
      final StatelessLambdaActor lambda = new StatelessLambdaActor(consumer);
      return use(() -> lambda); 
    }
    
    public <S> ActorSystem apply(Supplier<S> stateFactory, BiConsumer<Activation, S> consumer) {
      return use(() -> new StatefulLambdaActor<>(consumer, stateFactory.get()));
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
  
  public ActorSystem send(Message m) {
    final Activation a = activate(m.to());
    a.enqueue(m);
    return this;
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
  
  private Actor createActor(Object role) {
    final Supplier<Actor> factory = factories.get(role);
    if (factory == null) throw new IllegalArgumentException("No registered factory for actor of role " + role);
    return factory.get();
  }
  
  private static int getDefaultThreads() {
    final String defStr = System.getProperty("indigo.actorThreads", "0");
    final int defInt = Integer.parseInt(defStr);
    return defInt > 0 ? defInt : getNumProcessors() - defInt;
  }
  
  private static int getNumProcessors() {
    return Math.max(1, Runtime.getRuntime().availableProcessors());
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
