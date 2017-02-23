package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class ActorSystem implements Closeable {
  private final ExecutorService executor;
  
  private final Map<ActorId, Activation> activations = new ConcurrentHashMap<>();
  
  private final Map<Object, Supplier<Actor>> factories = new HashMap<>();
  
  public ActorSystem() {
    this(getDefaultThreads());
  }
  
  public ActorSystem(int numThreads) {
    executor = Executors.newWorkStealingPool(numThreads);
  }
  
  public ActorSystem of(Object type, Consumer<Message> consumer) {
    final LambdaActor lambda = new LambdaActor(consumer);
    return of(type, () -> lambda);
  }
  
  public ActorSystem of(Object type, Supplier<Actor> factory) {
    final Supplier<Actor> existing = factories.put(type, factory);
    if (existing != null) {
      factories.put(type, existing);
      throw new IllegalStateException("Factory for actor of type " + type + " has already been registered");
    }
    return this;
  }
  
  public void send(Message m) {
    final Activation a = activate(m.to());
    a.enqueue(m);
  }
  
  void dispatch(Activation a) {
    executor.execute(new DispatchTask(a));
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
          final Activation created = new Activation(id, this, createActor(id.getType()));
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
    executor.shutdown();
  }
}
