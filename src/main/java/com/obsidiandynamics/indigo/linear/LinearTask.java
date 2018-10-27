package com.obsidiandynamics.indigo.linear;

import java.util.concurrent.*;

/**
 *  A decorator for a task that must be executed in some deterministic partial order with
 *  respect to other tasks sharing a {@link LinearExecutor}. This interface does not define 
 *  the behaviour of the task — only the 'key', which associates the task with an ordered 
 *  set of peer tasks for serial execution in strict order of submission to the 
 *  {@link LinearExecutor}. <p>
 *  
 *  A {@link LinearTask} implementation should also extend either a {@link Runnable} or
 *  a {@link Callable} as appropriate (depending on the whether the task is expected
 *  to return a value and/or throw an exception). The {@link LinearRunnable}
 *  and {@link LinearCallable} interfaces are provided for this reason.
 */
public interface LinearTask {
  /**
   *  Returns a key that associates this task with an ordered set of peer tasks. Tasks
   *  sharing the same key will be executed serially, in strict order of submission to the
   *  {@link LinearExecutor}.
   *  
   *  @return The key {@link String}, which may not be {@code null}.
   */
  String getKey();
}
