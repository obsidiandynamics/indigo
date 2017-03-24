package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public final class ActorSystem {
  private final ActorSystemConfig config;
  
  private final ExecutorService executor;
  
  private final Map<ActorRef, Activation> activations;
  
  private final Map<String, ActorSetup> setupRegistry = new HashMap<>();
  
  private final LongAdder busyActors = new LongAdder();
  
  private final ActorRef ingressRef = ActorRef.of(INGRESS);
  
  private final TimeoutWatchdog timeoutWatchdog = new TimeoutWatchdog(this);
  
  private long nextActivationId = Crypto.machineRandom();
  
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
    executor = config.executor.apply(config.getParallelism());
    activations = new ConcurrentHashMap<>(16, .75f, config.getParallelism());
    when(ingressRef.role()).lambda(StatelessLambdaActor::agent);
    timeoutWatchdog.start();
  }
  
  public ActorSystem ingress(Consumer<Activation> act) {
    tell(ingressRef, act);
    return this;
  }
  
  public IngressBuilder ingress() {
    return new IngressBuilder();
  }
  
  public final class IngressBuilder {
    private int iterations = 1;
    
    public IngressBuilder times(int iterations) {
      this.iterations = iterations;
      return this;
    }
    
    public ActorSystem act(BiConsumer<Activation, Integer> act) {
      for (int i = 0; i < iterations; i++) {
        final int _i = i;
        ActorSystem.this.ingress(a -> act.accept(a, _i));
      }
      return ActorSystem.this;
    }
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
    
    public ActorSystem lambda(BiConsumer<Activation, Message> act) {
      return use(StatelessLambdaActor.builder().act(act)); 
    }
    
    public <S> ActorSystem lambda(Supplier<S> stateFactory, TriConsumer<Activation, Message, S> act) {
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
  
  public ActorSystem send(Message m) {
    for (;;) {
      final Activation a = activate(m.to());
      if (a._enqueue(m)) {
        return this;
      } else {
        m.to().setCachedActivation(null);
      }
    }
  }
  
  public void tell(ActorRef ref) {
    tell(ref, null);
  }
  
  public void tell(ActorRef ref, Object body) {
    send(new Message(null, ref, body, null, false));
  }
  
  public <T> CompletableFuture<T> ask(ActorRef ref) {
    return ask(ref, null);
  }
  
  /**
   *  Asks an actor for a given request, returning a future. This method is intended to be called
   *  from outside the actor system.<p>
   *  
   *  Although the consumer of the future is free to set their own timeout when calling <code>get()</code>,
   *  the actor system will impose a default upper bound on the timeout, after which a
   *  <code>TimeoutException</code> is thrown. To override this limit, call the overloaded
   *  <code>ask()</code> that takes a <code>timeoutMillisUpperBound</code> argument.
   *  
   *  @param ref The target actor.
   *  @param requestBody The request body.
   *  @return A future.
   */
  public <T> CompletableFuture<T> ask(ActorRef ref, Object requestBody) {
    return ask(ref, config.defaultAskTimeoutMillis, requestBody);
  }
  
  /**
   *  Asks an actor for a given request, returning a future. This method is intended to be called
   *  from outside the actor system.<p>
   *  
   *  This method provides the ability to override the default upper bound on the timeout that is
   *  set in <code>ActorSystemConfig</code>.
   *  
   *  @param ref The target actor.
   *  @param timeoutMillisUpperBound The upper bound on the timeout. Beyond this time, the future
   *                                 will yield a <code>TimeoutException</code>.
   *  @param requestBody The request body.
   *  @return A future.
   */
  public <T> CompletableFuture<T> ask(ActorRef ref, long timeoutMillisUpperBound, Object requestBody) {
    final CompletableFuture<T> f = new CompletableFuture<>();
    ingress(a -> 
      a.to(ref).ask(requestBody).await(timeoutMillisUpperBound)
      .onTimeout(() -> f.completeExceptionally(new TimeoutException()))
      .onResponse(r -> f.complete(r.body()))
    );
    return f;
  }
  
  public void _dispatch(Runnable r) {
    executor.execute(r);
  }
  
  public void _incBusyActors() {
    busyActors.increment();
  }
  
  public void _decBusyActors() {
    busyActors.decrement();
  }
  
  public ActorSystem drain() throws InterruptedException {
    while (busyActors.sum() != 0) {
      synchronized (busyActors) {
        busyActors.wait(10);
      }
    }
    return this;
  }
  
  private Activation activate(ActorRef ref) {
    final Activation cached = ref.getCachedActivation();
    if (cached != null) {
      return cached;
    }
    
    final Activation existing = activations.get(ref);
    if (existing != null) {
      ref.setCachedActivation(existing);
      return existing;
    } else {
      synchronized (activations) {
        final Activation existing2 = activations.get(ref);
        if (existing2 != null) {
          ref.setCachedActivation(existing2);
          return existing2;
        } else {
          final Activation created = createActivation(ref);
          activations.put(ref, created);
          ref.setCachedActivation(created);
          return created;
        }
      }
    }
  }
  
  public void _passivate(ActorRef ref) {
    activations.remove(ref);
  }
  
  TimeoutWatchdog getTimeoutWatchdog() {
    return timeoutWatchdog;
  }
  
  private Activation createActivation(ActorRef ref) {
    final ActorSetup setup = setupRegistry.get(ref.role());
    if (setup == null) throw new IllegalArgumentException("No setup for actor of role " + ref.role());
    final Actor actor = setup.factory.get();
    return setup.actorConfig.activationFactory.create(nextActivationId++, ref, this, setup.actorConfig, actor);
  }

  public void dispose() {
    while (true) {
      try {
        drain();
        break;
      } catch (InterruptedException e) {}
    }
    timeoutWatchdog.terminate();
    executor.shutdown();
  }
}
