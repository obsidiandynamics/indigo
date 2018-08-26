package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.task.*;
import com.obsidiandynamics.indigo.util.*;

public final class ActorSystem implements Endpoint {
  public static final String COMMON_EXECUTOR_NAME = "common";
  
  /** When draining, the maximum number of yields before putting the thread to sleep. */ 
  private static final int DRAIN_MAX_YIELDS = 10_000;
  
  /** When draining, the number of milliseconds to sleep between checks. */
  private static final long DRAIN_SLEEP_MILLIS = 1;
  
  /** A symbol for a task that's been cancelled. */
  private static final TimeoutTask CANCELLED = new TimeoutTask(0, new UUID(0, 0), null, null, null);
  
  private final long id = Crypto.machineRandom();
  
  private final ActorSystemConfig config;
  
  private final ExecutorService globalExecutor;
  
  private final Map<ActorRef, Activation> activations;
  
  private final Map<String, ActorSetup> setupRegistry = new HashMap<>();
  
  private final Integral64 busyActors;
  
  private final TaskScheduler timeoutScheduler = new TaskScheduler("TimeoutScheduler-" + getIdAsHex());
  
  private final TaskScheduler backgroundScheduler = new TaskScheduler("BackgroundScheduler-" + getIdAsHex());
  
  private final Reaper reaper = new Reaper(this);
  
  private final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
  
  private final BlockingQueue<Fault> deadLetterQueue = new LinkedBlockingQueue<>();
  
  private final Map<String, Executor> localExecutors = new HashMap<>();
  
  private final ActorRef[] ingressRefs;
  
  private long nextActivationId = Crypto.machineRandom();
  
  private volatile boolean shuttingDown;
  
  private volatile boolean running = true;
  
  private static final class ActorSetup {
    final Supplier<? extends Actor> factory;
    final ActorConfig actorConfig;
    ActorSetup(Supplier<? extends Actor> factory, ActorConfig actorConfig) {
      this.factory = factory;
      this.actorConfig = actorConfig;
    }
  }
  
  private ActorSystem(ActorSystemConfig config) {
    config.init();
    this.config = config;
    busyActors = config.integral64Provider.get();
    globalExecutor = config.executor.apply(new ExecutorParams(config.getParallelism(),
                                                              new JvmVersionProvider.DefaultProvider().get()));
    ingressRefs = createIngressRefs(config.getIngressCount());
    activations = new ConcurrentHashMap<>(16, .75f, config.getParallelism());
    registerStandardActors();
    registerStandardExecutors();
    timeoutScheduler.start();
    backgroundScheduler.start();
    reaper.init();
  }
  
  private void registerStandardActors() {
    on(INGRESS).cue(StatelessLambdaActor::agent);
    on(EGRESS).withConfig(new ActorConfig() {{ ephemeral = true; }}).cue(StatelessLambdaActor::agent);
  }
  
  private void registerStandardExecutors() {
    addExecutor(ForkJoinPool.commonPool()).named(COMMON_EXECUTOR_NAME);
  }
  
  String getIdAsHex() {
    return Long.toHexString(id);
  }
  
  private static ActorRef[] createIngressRefs(int ingressCount) {
    final ActorRef[] refs = new ActorRef[ingressCount];
    for (int i = 0; i < ingressCount; i++) { 
      refs[i] = ActorRef.of(INGRESS, String.valueOf(i));
    }
    return refs;
  }
  
  public ActorSystemConfig getConfig() {
    return config;
  }
  
  public ActorSystem ingress(Consumer<Activation> act) {
    final int randomIdx = ThreadLocalRandom.current().nextInt(ingressRefs.length);
    tell(ingressRefs[randomIdx], act);
    return this;
  }
  
  public IngressBuilder ingress() {
    return new IngressBuilder(this);
  }
  
  void registerActor(String role, Supplier<? extends Actor> factory, ActorConfig actorConfig) {
    final ActorSetup existing = setupRegistry.put(role, new ActorSetup(factory, actorConfig));
    if (existing != null) {
      setupRegistry.put(role, existing);
      throw new DuplicateRoleException("Factory for actor of role " + role + " has already been registered");
    }
  }
  
  public final class LocalExecutorBuilder {
    private final Executor executor;

    LocalExecutorBuilder(Executor executor) {
      this.executor = executor;
    }
    
    public ActorSystem named(String name) {
      final Executor existing = localExecutors.put(name, executor);
      if (existing != null) {
        localExecutors.put(name, existing);
        throw new DuplicateExecutorException("Executor named " + name + " has already been registered");
      }
      return ActorSystem.this;
    }
  }
  
  public LocalExecutorBuilder addExecutor(Executor executor) {
    return new LocalExecutorBuilder(executor);
  }
  
  public ActorBuilder on(String role) {
    return new ActorBuilder(this, role);
  }
  
  @Override
  public void send(Message message) {
    send(message, null);
  }
  
  void send(Message message, String preferredExecutorName) {
    final Executor preferredExecutor = preferredExecutorName != null ? getNamedExecutor(preferredExecutorName) : null;
    
    for (;;) {
      final Activation a = activate(message.to(), preferredExecutor);
      if (a.enqueue(message)) {
        return;
      } else {
        message.to().setCachedActivation(null);
      }
    }
  }
  
  Executor getNamedExecutor(String name) {
    final Executor executor = localExecutors.get(name);
    if (executor == null) {
      throw new NoSuchExecutorException("No executor for name " + name);
    }
    return executor;
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
   *  @param <T> The future's type.
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
   *  @param <T> The future's type.
   *  @param ref The target actor.
   *  @param timeoutMillisUpperBound The upper bound on the timeout. Beyond this time, the future
   *                                 will yield a <code>TimeoutException</code>.
   *  @param requestBody The request body.
   *  @return A future.
   */
  public <T> CompletableFuture<T> ask(ActorRef ref, long timeoutMillisUpperBound, Object requestBody) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    
    final AtomicBoolean taskRegion = new AtomicBoolean();
    final AwaitableAtomicReference<TimeoutTask> timeoutTaskHolder = new AwaitableAtomicReference<>();
    ingress(a -> {
      if (taskRegion.compareAndSet(false, true)) {
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
      }
    });
    
    future.whenComplete((t, x) -> {
      if (future.isCancelled()) {
        if (! taskRegion.compareAndSet(false, true)) {
          final TimeoutTask task = timeoutTaskHolder.awaitThenGet();
          // by forcing timeout, the pending task is removed from the ingress actor; calling this
          // a second (and subsequent) times has no further effect
          timeoutScheduler.executeNow(task);
          return;
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
  
  void incBusyActors() {
    busyActors.add(1);
  }
  
  void decBusyActors() {
    busyActors.add(-1);
  }
  
  void addError(Throwable t) {
    errors.add(t);
  }
  
  /**
   *  Determines whether this actor system is in the process of being shut down.
   *  
   *  @return True if shutdown is in progress.
   */
  public boolean isShuttingDown() {
    return shuttingDown;
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
    timeoutScheduler.forceExecute();
  }
  
  /**
   *  Waits until all actors have completely drained their mailbox backlog, returning an approximation
   *  of the number of backlogged actors.
   *  
   *  @param timeoutMillis The maximum amount of time to wait, or 0 for indefinite.
   *  @return The approximate number of backlogged actors remaining, in the range of 0 to the number of activated actors.
   *  @throws InterruptedException If the thread is interrupted.
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
        assert config.diagnostics.traceMacro("AS.drain: sum=%s, executor=%s\n", sum, globalExecutor);
        checkUncaughtExceptions();
        return sum.isCertain() ? sum.get() : Math.min(Math.max(1, sum.get()), activations.size());
      }
    }
  }
  
  private void checkUncaughtExceptions() {
    if (! errors.isEmpty()) {
      final List<Throwable> errorsReturn = new ArrayList<>(errors.size());
      for (;;) {
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
  
  private Activation activate(ActorRef ref, Executor preferredExecutor) {
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
          final Activation created = createActivation(ref, preferredExecutor != null ? preferredExecutor : globalExecutor);
          activations.put(ref, created);
          ref.setCachedActivation(created);
          return created;
        }
      }
    }
  }
  
  void dispose(ActorRef ref) {
    final Activation activation = activations.remove(ref);
    activation.dispose();
  }
  
  TaskScheduler getTimeoutScheduler() {
    return timeoutScheduler;
  }
  
  TaskScheduler getBackgroundScheduler() {
    return backgroundScheduler;
  }
  
  Reaper getReaper() {
    return reaper;
  }
  
  public void reap() {
    reaper.reap();
  }
  
  private Activation createActivation(ActorRef ref, Executor executor) {
    final ActorSetup setup = setupRegistry.get(ref.role());
    if (setup == null) throw new NoSuchRoleException("No setup for actor of role " + ref.role());
    final Actor actor = setup.factory.get();
    final Activation activation = setup.actorConfig.activationFactory.create(nextActivationId++, 
                                                                             ref, this, setup.actorConfig, 
                                                                             actor, executor);
    return activation;
  }
  
  /**
   *  Drains the actor system of any pending tasks and terminates it.<p>
   *  
   *  This method suppresses an <code>InterruptedException</code> and will re-assert the interrupt 
   *  prior to returning.
   */
  public void shutdownSilently() {
    shutdownSilently(true);
  }
  
  /**
   *  Shuts down the actor system with the option of draining it of any pending tasks.<p>
   *  
   *  This method suppresses an <code>InterruptedException</code> and will re-assert the interrupt 
   *  prior to returning.
   *  
   *  @param drain If set, will block until the actor system is drained of pending tasks.
   */
  public void shutdownSilently(boolean drain) {
    try {
      shutdown(drain);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  void terminate() {
    timeoutScheduler.clear();
    backgroundScheduler.clear();
    shutdownSilently(false);
  }
  
  /**
   *  Drains the actor system of any pending tasks and terminates it.
   *  
   *  @throws InterruptedException If the thread was interrupted.
   */
  public void shutdown() throws InterruptedException {
    shutdown(true);
  }

  /**
   *  Shuts down the actor system with the option of draining it of any pending tasks.
   *  
   *  @param drain If set, will block until the actor system is drained of pending tasks.
   *  @throws InterruptedException If the thread was interrupted.
   */
  public void shutdown(boolean drain) throws InterruptedException {
    shuttingDown = true;
    reaper.stop();
    if (drain) {
      for (;;) {
        drain(0);
        break;
      }
    }
    timeoutScheduler.forceExecute();
    timeoutScheduler.terminate();
    backgroundScheduler.forceExecute();
    backgroundScheduler.terminate();
    globalExecutor.shutdown();
    running = false;
  }
  
  public static ActorSystem create() {
    return create(new ActorSystemConfig());
  }
  
  public static ActorSystem create(ActorSystemConfig config) {
    return new ActorSystem(config);
  }
}
