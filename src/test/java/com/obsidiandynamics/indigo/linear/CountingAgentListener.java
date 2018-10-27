package com.obsidiandynamics.indigo.linear;

import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

final class CountingAgentListener implements AgentListener {
  private static final class Counts {
    final AtomicInteger activations = new AtomicInteger();
    final AtomicInteger passivations = new AtomicInteger();
    
    boolean isPending() {
      final int activationsCount = activations.get();
      final int passivationsCount = passivations.get();
      if (activationsCount != passivationsCount) {
        System.err.format(Counts.class.getSimpleName() + ".isPending(): activations=%d, passivations=%d\n", 
                          activationsCount, passivationsCount);
      }
      return activationsCount != passivationsCount;
    }

    @Override
    public String toString() {
      return Counts.class.getSimpleName() + " [activations=" + activations + ", passivations=" + passivations + "]";
    }
  }
  
  private final ConcurrentMap<String, Counts> counts = new ConcurrentHashMap<>();
  
  private Counts getCounts(String key) {
    return counts.computeIfAbsent(key, __ -> new Counts());
  }

  @Override
  public void agentActivated(String key) {
    getCounts(key).activations.incrementAndGet();
  }

  @Override
  public void agentPassivated(String key) {
    getCounts(key).passivations.incrementAndGet();
  }

  Map<String, Counts> getCounts() {
    return counts;
  }
  
  Map<String, Counts> getPendingCounts() {
    final Map<String, Counts> pendingCounts = new HashMap<>();
    for (Entry<String, Counts> countEntry : counts.entrySet()) {
      if (countEntry.getValue().isPending()) {
        pendingCounts.put(countEntry.getKey(), countEntry.getValue());
      }
    }
    return pendingCounts;
  }
}
