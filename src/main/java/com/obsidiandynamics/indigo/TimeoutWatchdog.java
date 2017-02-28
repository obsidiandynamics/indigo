package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

final class TimeoutWatchdog extends Thread {
  private static final int DEF_SLEEP = 1_000;
  
  private final ActorSystem system;
  
  private final SortedSet<TimeoutTask> timeouts = new ConcurrentSkipListSet<>();
  
  private final Object sleepLock = new Object();
  
  private volatile long nextWake;
  
  private volatile boolean running = true;
  
  TimeoutWatchdog(ActorSystem system) {
    super("TimeoutWatchdog");
    this.system = system;
    setDaemon(true);
  }
  
  void terminate() {
    running = false;
    synchronized (sleepLock) {
      sleepLock.notify();
    }
    try {
      join();
    } catch (InterruptedException e) {}
  }  
  
  @Override
  public void run() {
    while (running) {
      synchronized (sleepLock) {
        if (! timeouts.isEmpty()) {
          final TimeoutTask first = timeouts.first();
          delay(first.getExpiresAt());
        } else {
          delay(System.currentTimeMillis() + DEF_SLEEP);
        }
      }

      cycle();
    }
  }
  
  void enqueue(TimeoutTask task) {
    timeouts.add(task);
    final TimeoutTask first = safeGetFirst();
    if (first != null && first.getExpiresAt() < nextWake) {
      synchronized (sleepLock) {
        final TimeoutTask first2 = safeGetFirst();
        if (first2 != null && first2.getExpiresAt() < nextWake) {
          sleepLock.notify();
        }
      }
    }
  }
  
  private TimeoutTask safeGetFirst() {
    final Iterator<TimeoutTask> timeoutsIt = timeouts.iterator();
    return timeoutsIt.hasNext() ? timeoutsIt.next() : null;
  }
  
  private void delay(long until) {
    synchronized (sleepLock) {
      nextWake = until;
      while (running) {
        final long timeDiff = nextWake - System.currentTimeMillis();
        try {
          if (timeDiff > 0) {
            sleepLock.wait(Math.min(timeDiff, DEF_SLEEP));
          } else {
            break;
          }
        } catch (InterruptedException e) {}
      }
    }
  }
  
  private void cycle() {
    if (! timeouts.isEmpty()) {
      final TimeoutTask first = timeouts.first();
      if (System.currentTimeMillis() > first.getExpiresAt()) {
        timeouts.remove(first);
        if (! first.getRequest().isComplete()) {
          system.send(new Message(null, first.getActivation().self(), new TimeoutSignal(), first.getRequestId(), true));
        }
      }
    }
  }
}
