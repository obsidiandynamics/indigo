package com.obsidiandynamics.indigo.task;

import java.util.*;
import java.util.concurrent.*;

/**
 *  A scheduler for dispatching arbitrary tasks.
 */
public final class TaskScheduler extends Thread {
  /** Maximum sleep time. If the next task's time is longer, the sleep will be performed in a loop.
   *  This is also the default time that the scheduler sleeps for if it has no pending tasks. */
  private static final long MAX_SLEEP_NANOS = 1_000_000_000l;
  
  /** Minimum sleep time. Below this threshold sleeping isn't worthwhile. */
  private static final long MIN_SLEEP_NANOS = 1_000_000l;
  
  /** Compensation for the overhead of scheduling a task. */
  private static final long ADJ_NANOS = 500_000l;
  
  /** List of pending tasks, ordered with the most immediate at the head. */
  private final SortedSet<Task<?>> tasks = new ConcurrentSkipListSet<>();
  
  /** Lock for the scheduler thread to sleep on; can be used to wake the thread. */
  private final Object sleepLock = new Object();
  
  /** The time when the thread should be woken, in absolute nanoseconds. See {@link System.nanoTime()}. */
  private volatile long nextWake;
  
  /** Whether the scheduler thread should be running. */
  private volatile boolean running = true;
  
  /** Whether execution should be forced for all tasks (regarding of their scheduled time), pending and future. */
  private volatile boolean forceExecute;
  
  public TaskScheduler(String threadName) {
    super(threadName);
    setDaemon(true);
  }
  
  public void clear() {
    tasks.clear();
  }
  
  /**
   *  Terminates the scheduler, and awaits for its thread to end.
   *  
   *  @throws InterruptedException If the thread is interrupted.
   */
  public void terminate() throws InterruptedException {
    running = false;
    interrupt();
    if (Thread.interrupted()) throw new InterruptedException();
    join();
  }
  
  @Override
  public void run() {
    while (running) {
      synchronized (sleepLock) {
        if (! tasks.isEmpty()) {
          try {
            delay(tasks.first().getTime());
          } catch (NoSuchElementException e) {}
        } else {
          delay(System.nanoTime() + MAX_SLEEP_NANOS);
        }
      }

      if (Thread.interrupted()) return;

      cycle();
    }
  }
  
  /**
   *  Schedules a task for execution.
   *  
   *  @param task The task to schedule.
   */
  public void schedule(Task<?> task) {
    tasks.add(task);
    if (task.getTime() < nextWake) {
      synchronized (sleepLock) {
        if (task.getTime() < nextWake) {
          nextWake = task.getTime();
          sleepLock.notify();
        }
      }
    }
  }
  
  /**
   *  Removes the given task from the schedule. Once definitively removed, the scheduler will
   *  not execute the task.
   *  
   *  @param task The task to abort.
   *  @return Whether the task was in the schedule.
   */
  public boolean abort(Task<?> task) {
    return tasks.remove(task);
  }
  
  /**
   *  Puts the scheduler thread to sleep until the given time.
   *  
   *  @param until The wake time, in absolute nanoseconds (see {@link System#nanoTime()}).
   */
  private void delay(long until) {
    synchronized (sleepLock) {
      nextWake = until;
      while (running && ! forceExecute) {
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
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }
  
  /**
   *  Forces the immediate execution of all pending tasks, and all future tasks yet to be enqueued.
   */
  public void forceExecute() {
    forceExecute = true;
    synchronized (sleepLock) {
      sleepLock.notify();
    }
  }
  
  /**
   *  Schedules a single task, if one is pending and its time has come.
   */
  private void cycle() {
    if (! tasks.isEmpty()) {
      try {
        final Task<?> first = tasks.first();
        if (forceExecute || System.nanoTime() >= first.getTime() - ADJ_NANOS) {
          if (tasks.remove(first)) {
            first.execute();
          }
        }
      } catch (NoSuchElementException e) {} // in case the task was dequeued in the meantime
    }
  }
  
  /**
   *  Forces the execution of a given task.<p>
   *  
   *  This method is asynchronous, returning as soon as the resulting signal is enqueued.
   *  
   *  @param task The task to time out.
   */
  public void executeNow(Task<?> task) {
    if (abort(task)) {
      task.execute();
    }
  }
}
