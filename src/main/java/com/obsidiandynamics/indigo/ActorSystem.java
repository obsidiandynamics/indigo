package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.Activation.*;
import com.obsidiandynamics.indigo.ActorSystemConfig.*;
import com.obsidiandynamics.indigo.util.*;

public final class ActorSystem implements Endpoint {
  /** When draining, the maximum number of yields before putting the thread to sleep. */ 
  private static final int DRAIN_MAX_YIELDS = 10_000;
  
  /** When draining, the number of milliseconds to sleep between checks. */
  private static final long DRAIN_SLEEP_MILLIS = 1;
  
  /** A symbol for a task that's been cancelled. */
  private static final TimeoutTask CANCELLED = new TimeoutTask(0, new UUID(0, 0), null, null);
  
  private final ActorSystemConfig config;
  
  private final ExecutorService executor;
  
  private final Map<ActorRef, Activation> activations;
  
  private final Map<String, ActorSetup> setupRegistry = new HashMap<>();
  
  private final Integral64 busyActors = new Integral64.TripleStriped();
  
  private final ActorRef ingressRef = ActorRef.of(INGRESS);
  
  private final TimeoutWatchdog timeoutWatchdog = new TimeoutWatchdog(this);
  
  private final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
  
  private final BlockingQueue<Fault> deadLetterQueue = new LinkedBlockingQueue<>();
  
  private long nextActivationId = Crypto.machineRandom();
  
  private volatile boolean running = true;
  
  private static final class ActorSetup {
    final Supplier<Actor> factory;
    final ActorConfig actorConfig;
    ActorSetup(Supplier<Actor> factory, ActorConfig actorConfig) {
      this.factory = factory;
      this.actorConfig = actorConfig;
    }
  }
  
  ActorSystem(ActorSystemConfig config) {
    config.init();
    this.config = config;
    executor = config.executor.apply(new ExecutorParams(config.getParallelism(),
                                                        new JvmVersionProvider.DefaultProvider().get()));
    activations = new ConcurrentHashMap<>(16, .75f, config.getParallelism());
    when(ingressRef.role()).lambda(StatelessLambdaActor::agent);
    timeoutWatchdog.start();
  }
  
  public ActorSystemConfig getConfig() {
    return config;
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
    
    public ActorSystem act(Consumer<Activation> act) {
      return act((a, i) -> act.accept(a));
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
    
    public <S> ActorSystem lambdaSync(Supplier<S> stateFactory, TriConsumer<Activation, Message, S> act) {
      return use(StatefulLambdaActor.<S>builder().act(act).activated(a -> CompletableFuture.completedFuture(stateFactory.get())));
    }
    
    public <S> ActorSystem lambdaSync(Function<Activation, S> stateFactory, TriConsumer<Activation, Message, S> act) {
      return use(StatefulLambdaActor.<S>builder().act(act).activated(a -> CompletableFuture.completedFuture(stateFactory.apply(a))));
    }
    
    public <S> ActorSystem lambdaAsync(Function<Activation, CompletableFuture<S>> futureStateFactory, TriConsumer<Activation, Message, S> act) {
      return use(StatefulLambdaActor.<S>builder().act(act).activated(futureStateFactory));
    }
    
    public ActorSystem use(Supplier<Actor> factory) {
      register(role, factory, actorConfig);
      return ActorSystem.this;
    }
    
    private void register(String role, Supplier<Actor> factory, ActorConfig actorConfig) {
      final ActorSetup existing = setupRegistry.put(role, new ActorSetup(factory, actorConfig));
      if (existing != null) {
        setupRegistry.put(role, existing);
        throw new DuplicateRoleException("Factory for actor of role " + role + " has already been registered");
      }
    }
  }
  
  public ActorBuilder when(String role) {
    return new ActorBuilder(role);
  }
  
  @Override
  public void send(Message message) {
    for (;;) {
      final Activation a = activate(message.to());
      if (a.enqueue(message)) {
        return;
      } else {
        message.to().setCachedActivation(null);
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
    final CompletableFuture<T> future = new CompletableFuture<>();
    
    final AtomicBoolean taskRegion = new AtomicBoolean();
    final AtomicReference<TimeoutTask> timeoutTaskHolder = new AtomicReference<>();
    ingress(a -> {
      if (taskRegion.compareAndSet(false, true)) {
        if (! future.isCancelled()) {
          final MessageBuilder mb = a.to(ref);
          mb.ask(requestBody).await(timeoutMillisUpperBound)
          .onTimeout(() -> future.completeExceptionally(new TimeoutException()))
          .onFault(f -> {
            if (f.getReason() instanceof Throwable) {
              future.completeExceptionally(f.getReason());
            } else {
              future.completeExceptionally(new FaultException(f.getReason()));
            }
          })
          .onResponse(r -> future.complete(r.body()));
          timeoutTaskHolder.set(mb.getTimeoutTask());
        } else {
          timeoutTaskHolder.set(CANCELLED);
        }
      }
    });
    
    future.whenComplete((t, x) -> {
      if (future.isCancelled()) {
        if (! taskRegion.compareAndSet(false, true)) {
          for (;;) {
            final TimeoutTask task = timeoutTaskHolder.get();
            if (task != null) {
              // by forcing timeout, the pending task is removed from the ingress actor; calling this
              // a second (and subsequent) times has no further effect
              timeoutWatchdog.timeout(task);
              return;
            } else {
              Thread.yield();
            }
          }
        } else {
          timeoutTaskHolder.set(CANCELLED);
        }
      }
    });
    return future;
  }
  
  void addToDeadLetterQueue(Fault fault) {
    deadLetterQueue.add(fault);
    if (deadLetterQueue.size() > config.deadLetterQueueSize) {
      deadLetterQueue.poll();
    }
  }
  
  public List<Fault> getDeadLetterQueue() {
    return Collections.unmodifiableList(Arrays.asList(deadLetterQueue.toArray(new Fault[0])));
  }
  
  public void _dispatch(Runnable r) {
    executor.execute(() -> {
      try {
        r.run();
      } catch (Throwable t) {
        config.exceptionHandler.accept(this, t);
        running = false;
        timeoutWatchdog.terminate();
        executor.shutdownNow();
      }
    });
  }
  
  public void _incBusyActors() {
    busyActors.add(1);
  }
  
  public void _decBusyActors() {
    busyActors.add(-1);
  }
  
  void addError(Throwable t) {
    errors.add(t);
  }
  
  /**
   *  Determines whether the actor system is operational.
   *  
   *  @return True if the system is operational; false if it has been shut down.
   */
  public boolean isRunning() {
    return running;
  }
  
  /**
   *  Forces the timeout of any in-flight request-response style messages, as well as any future
   *  messages enqueued after this call.
   */
  public void forceTimeout() {
    timeoutWatchdog.forceTimeout();
  }
  
  /**
   *  Waits until all actors have completely drained their mailbox backlog, returning an approximation
   *  of the number of backlogged actors.
   *  
   *  @param timeoutMillis The maximum amount of time to wait, or 0 for indefinite.
   *  @return The approximate number of backlogged actors remaining, in the range of 0 to the number of activated actors.
   *  @throws InterruptedException
   *  @throws UnhandledMultiException If any unhandled exceptions were accumulated.
   */
  public long drain(long timeoutMillis) throws InterruptedException {
    final long deadline = timeoutMillis != 0 ? System.currentTimeMillis() + timeoutMillis : 0;
    final Integral64.Sum sum = new Integral64.Sum();
    int yields = DRAIN_MAX_YIELDS;
    for (;;) {
      if (Thread.interrupted()) throw new InterruptedException();
      
      busyActors.sum(sum);
      if (! running || (sum.isCertain() && sum.get() == 0)) {
        checkUncaughtExceptions();
        return 0;
      } else if (yields > 0) {
        Thread.yield();
        yields--;
      } else {
        Thread.sleep(DRAIN_SLEEP_MILLIS);
      }
      
      if (deadline != 0 && System.currentTimeMillis() > deadline) {
        busyActors.sum(sum);
        assert config.diagnostics.traceMacro("AS.drain: sum=%s, executor=%s\n", sum, executor);
        checkUncaughtExceptions();
        return sum.isCertain() ? sum.get() : Math.min(Math.max(1, sum.get()), activations.size());
      }
    }
  }
  
  private void checkUncaughtExceptions() {
    if (! errors.isEmpty()) {
      final List<Throwable> errorsReturn = new ArrayList<>(errors.size());
      while (! errors.isEmpty()) {
        final Throwable error = errors.poll();
        if (error != null) {
          errorsReturn.add(error);
        } else {
          break;
        }
      }
      throw new UnhandledMultiException(errorsReturn.toArray(new Throwable[errorsReturn.size()]));
    }
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
  
  public void _dispose(ActorRef ref) {
    activations.remove(ref);
  }
  
  TimeoutWatchdog getTimeoutWatchdog() {
    return timeoutWatchdog;
  }
  
  private Activation createActivation(ActorRef ref) {
    final ActorSetup setup = setupRegistry.get(ref.role());
    if (setup == null) throw new NoSuchRoleException("No setup for actor of role " + ref.role());
    final Actor actor = setup.factory.get();
    return setup.actorConfig.activationFactory.create(nextActivationId++, ref, this, setup.actorConfig, actor);
  }
  
  /**
   *  Drains the actor system of any pending tasks and terminates it.<p>
   *  
   *  This method suppresses an <code>InterruptedException</code> and will re-assert the interrupt 
   *  prior to returning.
   */
  public void shutdownQuietly() {
    try {
      shutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  /**
   *  Drains the actor system of any pending tasks and terminates it.
   *  
   *  @throws InterruptedException
   */
  public void shutdown() throws InterruptedException {
    for (;;) {
      drain(0);
      break;
    }
    timeoutWatchdog.forceTimeout();
    timeoutWatchdog.terminate();
    executor.shutdown();
    running = false;
  }
}
