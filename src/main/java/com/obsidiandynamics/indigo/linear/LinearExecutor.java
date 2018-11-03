package com.obsidiandynamics.indigo.linear;

import java.util.*;
import java.util.concurrent.*;

/**
 *  A variation of {@link ExecutorService}, enabling the caller to stipulate an
 *  explicit ordering of submitted tasks on a basis of a free-form key, defining a 
 *  partial execution order of tasks among their peers. <p>
 *  
 *  Tasks are conventional {@link Runnable} and {@link Callable} derivatives, having also 
 *  implemented a conditionally optional {@link LinearTask} decorator interface. The latter 
 *  exposes a single string-based 'key', that forms a logical association among the
 *  submitted tasks sharing the same key. For any given key, tasks will be executed
 *  serially, in the strict order of task submission to the {@link LinearExecutor}. In other
 *  words, tasks sharing a common key are totally ordered. No relative order is imposed on
 *  tasks with differing keys — they may execute in parallel and in arbitrary order relative
 *  to one another, as per conventional thread-pooled behaviour. <p>
 *  
 *  While the developer can subclass {@link LinearTask} to decorate submissions, the recommended
 *  (and easiest) way to submit ordered tasks is to wrap conventional {@link Runnable}s and 
 *  {@link Callable}s using the {@link LinearRunnable#decorate(Runnable, String)} and 
 *  {@link LinearCallable#decorate(Callable, String)} helper methods.
 */
public interface LinearExecutor {
  /**
   *  Attempts to stop all actively executing tasks, halts the processing of waiting tasks, 
   *  and returns a list of the tasks that were awaiting execution. <p>
   *  
   *  Unlike {@link ExecutorService#shutdownNow()}, this variant lets the caller
   *  specify whether the remaining tasks should be interrupted. Additionally, it returns
   *  a {@link List} of {@link LinearFutureTask}s, which is often more convenient, allowing
   *  the caller to interrogate the tasks directly.
   *  
   *  @param mayInterruptIfRunning Whether the aborted tasks should be interrupted.
   *  @return A {@link List} of pending {@link LinearFutureTask}s.
   */
  public List<LinearFutureTask<?>> shutdownNow(boolean mayInterruptIfRunning);

  /**
   *  Obtains a count of the number of submitted tasks that are still pending 
   *  execution.
   *  
   *  @return The number of pending tasks.
   */
  int getPendingTasks();
}
