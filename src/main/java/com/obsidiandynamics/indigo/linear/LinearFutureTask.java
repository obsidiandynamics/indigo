package com.obsidiandynamics.indigo.linear;

import static com.obsidiandynamics.func.Functions.*;

import java.util.concurrent.*;

import com.obsidiandynamics.func.*;

/**
 *  Encapsulation of a task that is scheduled to be executed within a {@link LinearExecutor}. <p>
 *  
 *  This task should only be instantiated by the {@link LinearExecutor} instance, but is 
 *  subsequently made available to the application so that it can be interrogated directly.
 *  
 *  @param <V> Result type.
 */
public final class LinearFutureTask<V> implements RunnableFuture<V>, LinearTask {
  private final LinearExecutor executor;
  
  private final FutureTask<V> delegateTask;
  
  private final String key;
  
  LinearFutureTask(LinearExecutor executor, FutureTask<V> delegateTask, String key) {
    mustExist(key, withMessage("Key cannot be null", NullArgumentException::new));
    this.executor = executor;
    this.delegateTask = delegateTask;
    this.key = key;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return delegateTask.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return delegateTask.isCancelled();
  }

  @Override
  public boolean isDone() {
    return delegateTask.isDone();
  }
  
  /**
   *  Determines whether the task has completed successfully, such that a result can now
   *  be obtained using {@link Future#get()} without blocking.
   *  
   *  @return True if the task has completed successfully.
   */
  public boolean isCompletedSuccessfully() {
    return delegateTask.isDone() && canGetSuccessfully();
  }
  
  /**
   *  Determines whether this task has completed exceptionally, in any way. Possible causes include
   *  cancellations and execution errors.
   *
   *  @return True if the task has completed exceptionally.
   */
  public boolean isCompletedExceptionally() {
    return delegateTask.isDone() && ! canGetSuccessfully();
  }
  
  private boolean canGetSuccessfully() {
    try {
      delegateTask.get();
      return true;
    } catch (Throwable e) {
      return false;
    }
  }
  
  /**
   *  Awaits the completion of the task, bounded by the specified timeout. This method returns the status
   *  upon completion, as per the {@link #isCompletedSuccessfully()} method.
   *  
   *  @param timeout The time to wait.
   *  @param unit The time unit.
   *  @return True if the task has completed successfully.
   *  @throws InterruptedException If the thread was interrupted.
   *  @throws TimeoutException If the wait operation timed out.
   */
  public boolean awaitDone(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    try {
      delegateTask.get(timeout, unit);
      return true;
    } catch (ExecutionException | CancellationException e) {
      return false;
    }
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return delegateTask.get();
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return delegateTask.get(timeout, unit);
  }

  @Override
  public void run() {
    delegateTask.run();
    executor.markCompleted(this);
  }

  @Override
  public String getKey() {
    return key;
  }
  
  @Override
  public String toString() {
    return LinearFutureTask.class.getSimpleName() + " [key=" + key + "]";
  }
}
