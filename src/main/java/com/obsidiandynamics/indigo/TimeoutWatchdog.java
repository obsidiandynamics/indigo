package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.concurrent.*;

final class TimeoutWatchdog {
  private static class TimeoutTask implements Comparable<TimeoutTask> {
    final long expiresAt;
    
    final UUID requestId;
    
    final Activation activation;

    TimeoutTask(long expiresAt, UUID requestId, Activation activation) {
      this.expiresAt = expiresAt;
      this.requestId = requestId;
      this.activation = activation;
    }

    @Override
    public int compareTo(TimeoutTask o) {
      final int expiresComp = Long.compare(expiresAt, o.expiresAt);
      return expiresComp != 0 ? expiresComp : requestId.compareTo(o.requestId);
    }
  }
  
  private final SortedSet<TimeoutTask> timouts = new ConcurrentSkipListSet<>();
}
