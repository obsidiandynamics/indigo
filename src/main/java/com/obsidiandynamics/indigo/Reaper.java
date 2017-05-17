package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.task.*;
import com.obsidiandynamics.indigo.util.*;

final class Reaper {
  private final ActorSystem system;
  
  private final Set<Activation> activations = new CopyOnWriteArraySet<>();
  
  private final Object reaperLock = new Object();
  
  private boolean running = true;
  
  Reaper(ActorSystem system) {
    this.system = system;
  }
  
  void init() {
    scheduleNext();
  }
  
  void stop() {
    synchronized (reaperLock) {
      running = false;
    }
  }
  
  void register(Activation activation) {
    activations.add(activation);
  }
  
  void deregister(Activation activation) {
    activations.remove(activation);
  }
  
  boolean isReapingEnabled() {
    return system.getConfig().reaperPeriodMillis != 0;
  }
  
  private void scheduleNext() {
    if (isReapingEnabled() && ! system.isShuttingDown()) {
      final long nextReap = System.nanoTime() + system.getConfig().reaperPeriodMillis * 1_000_000l;
      system.getBackgroundScheduler().schedule(new Task<Long>(nextReap, Crypto.machineRandom()) {
        @Override protected void execute() {
          Threads.asyncDaemon(Reaper.this::reapAndReschedule, "Reaper-" + system.getIdAsHex());
        }
      });
    }
  }
  
  private void reapAndReschedule() {
    reap();
    scheduleNext();
  }
  
  void reap() {
    synchronized (reaperLock) {
      if (running) {
        final long now = System.currentTimeMillis();
        for (Activation a : activations) {
          final long overdueBy = now - a.getLastMessageTime() - a.actorConfig.reapTimeoutMillis;
          if (overdueBy > 0) {
            assert system.getConfig().diagnostics.traceMacro("R.reap: ref=%s, overdue=%d\n", a.ref, overdueBy);
            a.enqueue(new Message(null, a.ref, SleepingPill.instance(), null, false));
          }
        }
      }
    }
  }
}
