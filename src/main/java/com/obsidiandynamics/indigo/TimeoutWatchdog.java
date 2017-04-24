package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

/**
 *  A scheduler for dispatching timeout signals back to actors that are waiting for a
 *  response to a message.
 */
final class TimeoutWatchdog extends Thread {
  /** A pre-instantiated timeout signal (all timeout signals are the same). */
  private static final Timeout TIMEOUT_SIGNAL = new Timeout();
  
  /** Maximum sleep time. If the expiry time is longer, the sleep will be performed in a loop.
   *  This is also the default time that the timer sleeps for if it has no timeout tasks. */
  private static final long MAX_SLEEP_NANOS = 1_000_000_000l;
  
  /** Minimum sleep time. Below this threshold sleeping isn't worthwhile. */
  private static final long MIN_SLEEP_NANOS = 1_000_000l;
  
  /** Compensation for overhead of scheduling a timeout message with the dispatcher. */
  private static final long ADJ_NANOS = 500_000l;
  
  /** Endpoint to deliver the signals to; normally an {@link ActorSystem}. */
  private final Endpoint endpoint;
  
  /** List of timeout tasks, ordered with the most immediate at the head. */
  private final SortedSet<TimeoutTask> timeouts = new ConcurrentSkipListSet<>(TimeoutTask::byExpiry);
  
  /** Lock for the watchdog thread to sleep on; can be used to wake the thread. */
  private final Object sleepLock = new Object();
  
  /** The time when the thread should be woken, in absolute nanoseconds. See {@link System.nanoTime()}. */
  private volatile long nextWake;
  
  /** Whether the watchdog thread should be running. */
  private volatile boolean running = true;
  
  /** Whether timeouts should be forced for all tasks, pending and future. */
  private volatile boolean forceTimeout;
  
  TimeoutWatchdog(Endpoint endpoint) {
    super("TimeoutWatchdog");
    this.endpoint = endpoint;
    setDaemon(true);
  }
  
  /**
   *  Terminates the watchdog and blocks until the underlying thread joins.<p>
   *  
   *  This method will not return immediately on an interrupt and will strive to run to
   *  conclusion, but will re-assert the interrupt prior to eventually returning.
   */
  void terminate() {
    running = false;
    synchronized (sleepLock) {
      sleepLock.notify();
    }
    
    boolean interrupted = false;
    try {
      for (;;) {
        try {
          join();
          return;
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }  
  
  void terminateForcibly() {
    running = false;
    synchronized (sleepLock) {
      sleepLock.notify();
    }
  }
  
  @Override
  public void run() {
    while (running) {
      synchronized (sleepLock) {
        if (! timeouts.isEmpty()) {
          try {
            delay(timeouts.first().getExpiresAt());
          } catch (NoSuchElementException e) {}
        } else {
          delay(System.nanoTime() + MAX_SLEEP_NANOS);
        }
      }

      cycle();
    }
  }
  
  /**
   *  Schedules a task for a timeout signal.
   *  
   *  @param task The task to schedule.
   */
  void schedule(TimeoutTask task) {
    timeouts.add(task);
    if (task.getExpiresAt() < nextWake) {
      synchronized (sleepLock) {
        if (task.getExpiresAt() < nextWake) {
          nextWake = task.getExpiresAt();
          sleepLock.notify();
        }
      }
    }
  }
  
  /**
   *  Aborts a given task from the timeout schedule. Once definitively removed, the watchdog will
   *  not timeout the task.
   *  
   *  @param task The task to abort.
   *  @return Whether the task was in the schedule.
   */
  boolean abort(TimeoutTask task) {
    return timeouts.remove(task);
  }
  
  /**
   *  Puts the watchdog thread to sleep until the given time.
   *  
   *  @param until The wake time, in absolute nanoseconds (see {@link System.nanoTime()}).
   */
  private void delay(long until) {
    boolean interrupted = false;
    try {
      synchronized (sleepLock) {
        nextWake = until;
        while (running && ! forceTimeout) {
          final long timeDiff = Math.min(MAX_SLEEP_NANOS, nextWake - System.nanoTime() - ADJ_NANOS);
          try {
            if (timeDiff >= MIN_SLEEP_NANOS) {
              final long millis = timeDiff / 1_000_000l;
              final int nanos = (int) (timeDiff - millis * 1_000_000l);
              sleepLock.wait(millis, nanos);
            } else {
              break;
            }
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  /**
   *  Forces the timeout of all pending tasks, and all future tasks yet to be enqueued.
   */
  void forceTimeout() {
    forceTimeout = true;
    synchronized (sleepLock) {
      sleepLock.notify();
    }
  }
  
  /**
   *  Times out a single task, if one is pending and its time has come.
   */
  private void cycle() {
    if (! timeouts.isEmpty()) {
      try {
        final TimeoutTask first = timeouts.first();
        if (forceTimeout || System.nanoTime() >= first.getExpiresAt() - ADJ_NANOS) {
          final boolean removed = timeouts.remove(first);
          if (removed && ! first.getRequest().isComplete()) {
            endpoint.send(new Message(null, first.getActorRef(), TIMEOUT_SIGNAL, first.getRequestId(), true));
          }
        }
      } catch (NoSuchElementException e) {} // in case the task was dequeued in the meantime
    }
  }
  
  /**
   *  Forces the timeout of a given task.<p>
   *  
   *  This method is asynchronous, returning as soon as the resulting signal is enqueued.
   *  
   *  @param timeoutTask The task to time out.
   */
  void timeout(TimeoutTask timeoutTask) {
    if (abort(timeoutTask)) {
      endpoint.send(new Message(null, timeoutTask.getActorRef(), TIMEOUT_SIGNAL, timeoutTask.getRequestId(), true));
    }
  }
}
