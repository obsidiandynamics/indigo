package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public final class ActorSystem {
  private final ActorSystemConfig config;
  
  private final ExecutorService executor;
  
  private final Map<ActorRef, Activation> activations = new ConcurrentHashMap<>();
  
  private final Map<String, ActorSetup> setupRegistry = new HashMap<>();
  
  private final AtomicInteger busyActors = new AtomicInteger();
  
  private final AtomicLong backlog = new AtomicLong();
  
  private final ActorRef ingressRef = ActorRef.of("_ingress");
  
  private final TimeoutWatchdog timeoutWatchdog = new TimeoutWatchdog(this);
  
  private static final class ActorSetup {
    final Supplier<Actor> factory;
    final ActorConfig actorConfig;
    ActorSetup(Supplier<Actor> factory, ActorConfig actorConfig) {
      this.factory = factory;
      this.actorConfig = actorConfig;
    }
  }
  
  ActorSystem(ActorSystemConfig config) {
    this.config = config;
    executor = Executors.newFixedThreadPool(config.numThreads);
    when(ingressRef.role()).configure(new ActorConfig() {{
      // if we throttle with only one thread in the pool, then the ingress will cause a deadlock
      // under throttling conditions
      throttleSend = config.numThreads >= 2;
    }})
    .lambda(StatelessLambdaActor::agent);
    timeoutWatchdog.start();
  }
  
  public ActorSystem ingress(Consumer<Activation> act) {
    return send(new Message(null, ingressRef, act, null, false), true);
  }
  
  public final class ActorBuilder {
    private final String role;
    private ActorConfig actorConfig = config.defaultActorConfig;
    
    ActorBuilder(String role) { 
      this.role = role;
    }
    
    public ActorBuilder configure(ActorConfig actorConfig) {
      this.actorConfig = actorConfig;
      return this;
    }
    
    public ActorSystem lambda(Consumer<Activation> act) {
      return use(StatelessLambdaActor.builder().act(act)); 
    }
    
    public <S> ActorSystem lambda(Supplier<S> stateFactory, BiConsumer<Activation, S> act) {
      return use(StatefulLambdaActor.<S>builder().act(act).activated(a -> stateFactory.get()));
    }
    
    public ActorSystem use(Supplier<Actor> factory) {
      register(role, factory, actorConfig);
      return ActorSystem.this;
    }
  }
  
  public ActorBuilder when(String role) {
    return new ActorBuilder(role);
  }
  
  private void register(String role, Supplier<Actor> factory, ActorConfig actorConfig) {
    final ActorSetup existing = setupRegistry.put(role, new ActorSetup(factory, actorConfig));
    if (existing != null) {
      setupRegistry.put(role, existing);
      throw new IllegalStateException("Factory for actor of role " + role + " has already been registered");
    }
  }
  
  ActorSystem send(Message m, boolean throttle) {
    if (throttle) throttleBacklog(m.from());
    
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
    while (backlog.get() > config.backlogCapacity) {
      try {
        Thread.sleep(config.backlogThrottleMillis);
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
          final Activation created = createActivation(ref);
          activations.put(ref, created);
          return created;
        }
      }
    }
  }
  
  void passivate(ActorRef ref) {
    activations.remove(ref);
  }
  
  TimeoutWatchdog getTimeoutWatchdog() {
    return timeoutWatchdog;
  }
  
  private Activation createActivation(ActorRef ref) {
    final ActorSetup setup = setupRegistry.get(ref.role());
    if (setup == null) throw new IllegalArgumentException("No setup for actor of role " + ref.role());
    final Actor actor = setup.factory.get();
    return new Activation(ref, this, setup.actorConfig, actor);
  }

  public void shutdown() {
    while (true) {
      try {
        await();
        break;
      } catch (InterruptedException e) {}
    }
    timeoutWatchdog.terminate();
    executor.shutdown();
  }
}
