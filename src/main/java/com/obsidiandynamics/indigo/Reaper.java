package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.task.*;
import com.obsidiandynamics.indigo.util.*;

final class Reaper {
  private final ActorSystem system;
  
  private final Set<Activation> activations = new CopyOnWriteArraySet<>();
  
  private volatile boolean running = true;
  
  Reaper(ActorSystem system) {
    this.system = system;
  }
  
  void init() {
    scheduleNext();
  }
  
  void terminate() {
    running = false;
  }
  
  private void scheduleNext() {
    if (running) {
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
    //TODO
  }
}
