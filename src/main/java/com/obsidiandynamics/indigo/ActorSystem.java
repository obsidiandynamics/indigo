package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public final class ActorSystem implements Closeable {
  private final ExecutorService executor;
  
  private final Map<ActorId, Activation> activations = new ConcurrentHashMap<>();
  
  private final Map<Object, Supplier<Actor>> factories = new HashMap<>();
  
  private final AtomicInteger busyActors = new AtomicInteger();
  
  private final ActorId rootId = ActorId.of("_root", 0);
  
  private final Activation rootActivation;
  
  public ActorSystem() {
    this(getDefaultThreads());
  }
  
  public ActorSystem(int numThreads) {
    executor = Executors.newWorkStealingPool(numThreads);
    register(rootId.type(), () -> new LambdaActor(a -> {}));
    rootActivation = activate(rootId);
  }
  
  public ActorSystem ingress(Consumer<Activation> consumer) {
    consumer.accept(rootActivation);
    return this;
  }
  
  public final class ActorBuilder {
    private final Object type;
    
    ActorBuilder(Object type) { 
      this.type = type;
    }
    
    public ActorSystem apply(Consumer<Activation> consumer) {
      final LambdaActor lambda = new LambdaActor(consumer);
      return use(() -> lambda); 
    }
    
    public ActorSystem use(Supplier<Actor> factory) {
      register(type, factory);
      return ActorSystem.this;
    }
  }
  
  public ActorBuilder when(Object type) {
    return new ActorBuilder(type);
  }
  
  private void register(Object type, Supplier<Actor> factory) {
    final Supplier<Actor> existing = factories.put(type, factory);
    if (existing != null) {
      factories.put(type, existing);
      throw new IllegalStateException("Factory for actor of type " + type + " has already been registered");
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
  
  private Activation activate(ActorId id) {
    final Activation existing = activations.get(id);
    if (existing != null) {
      return existing;
    } else {
      synchronized (activations) {
        final Activation existing2 = activations.get(id);
        if (existing2 != null) {
          return existing2;
        } else {
          final Activation created = new Activation(id, this, createActor(id.type()));
          activations.put(id, created);
          return created;
        }
      }
    }
  }
  
  private Actor createActor(Object type) {
    final Supplier<Actor> factory = factories.get(type);
    if (factory == null) throw new IllegalArgumentException("No registered factory for actor of type " + type);
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

  @Override
  public void close() {
    while (true) {
      try {
        await();
        break;
      } catch (InterruptedException e) {}
    }
    executor.shutdown();
  }
}
