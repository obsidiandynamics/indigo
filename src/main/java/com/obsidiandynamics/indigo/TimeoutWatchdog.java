package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

final class TimeoutWatchdog extends Thread {
  private static final Timeout TIMEOUT_SIGNAL = new Timeout();
  
  /** Maximum sleep time. If the expiry time is longer, the sleep will be performed in a loop.
   *  This is also the default time that the timer sleeps for if it has no timeout tasks. */
  private static final long MAX_SLEEP_NANOS = 1_000_000_000l;
  
  /** Minimum sleep time. Below this threshold sleeping isn't worthwhile. */
  private static final long MIN_SLEEP_NANOS = 1_000_000l;
  
  /** Compensation for overhead of scheduling a timeout message with the dispatcher. */
  private static final long ADJ_NANOS = 500_000l;
  
  private final ActorSystem system;
  
  private final SortedSet<TimeoutTask> timeouts = new ConcurrentSkipListSet<>(TimeoutTask::byExpiry);
  
  private final Object sleepLock = new Object();
  
  private volatile long nextWake;
  
  private volatile boolean running = true;
  
  private volatile boolean forceTimeout;
  
  TimeoutWatchdog(ActorSystem system) {
    super("TimeoutWatchdog");
    this.system = system;
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
  
  void enqueue(TimeoutTask task) {
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
  
  boolean dequeue(TimeoutTask task) {
    return timeouts.remove(task);
  }
  
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
  
  void forceTimeout() {
    forceTimeout = true;
    synchronized (sleepLock) {
      sleepLock.notify();
    }
  }
  
  private void cycle() {
    if (! timeouts.isEmpty()) {
      try {
        final TimeoutTask first = timeouts.first();
        if (forceTimeout || System.nanoTime() >= first.getExpiresAt() - ADJ_NANOS) {
          timeouts.remove(first);
          if (! first.getRequest().isComplete()) {
            system.send(new Message(null, first.getActivation().self(), TIMEOUT_SIGNAL, first.getRequestId(), true));
          }
        }
      } catch (NoSuchElementException e) {} // in case the task was dequeued in the meantime
    }
  }
  
  void timeout(TimeoutTask timeoutTask) {
    if (dequeue(timeoutTask)) {
      system.send(new Message(null, timeoutTask.getActivation().self(), TIMEOUT_SIGNAL, timeoutTask.getRequestId(), true));
    }
  }
}
