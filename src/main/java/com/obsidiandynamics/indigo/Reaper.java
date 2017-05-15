package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.task.*;
import com.obsidiandynamics.indigo.util.*;

final class Reaper {
  private final ActorSystem system;
  
  private final Set<Activation> activations = new CopyOnWriteArraySet<>();
  
  Reaper(ActorSystem system) {
    this.system = system;
  }
  
  void init() {
    scheduleNext();
  }
  
  void register(Activation activation) {
    if (isReapingEnabled()) {
      activations.add(activation);
    }
  }
  
  void deregister(Activation activation) {
    if (isReapingEnabled()) {
      activations.remove(activation);
    }
  }
  
  private boolean isReapingEnabled() {
    return system.getConfig().reaperPeriodMillis != 0;
  }
  
  private void scheduleNext() {
    if (isReapingEnabled() && ! system.isShuttingDown()) {
      final long nextReap = System.nanoTime() + system.getConfig().reaperPeriodMillis * 1_000_000l;
      system.getBackgroundScheduler().schedule(new Task<Long>(nextReap, Crypto.machineRandom()) {
        @Override
        protected void execute() {
          reap();
          scheduleNext();
        }
      });
    }
  }
  
  private void reap() {
    for (Activation a : activations) {
      a.enqueue(new Message(null, a.ref, SleepingPill.instance(), null, false));
    }
  }
}
