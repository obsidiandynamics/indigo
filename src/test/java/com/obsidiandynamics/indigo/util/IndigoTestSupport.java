package com.obsidiandynamics.indigo.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.*;

public interface IndigoTestSupport extends TestSupport {
  default BiConsumer<Activation, Message> refCollector(Set<ActorRef> set) {
    return (a, m) -> {
      log("Finished %s\n", m.from());
      set.add(m.from());
    };
  }
  
  static int countFaults(FaultType type, Collection<Fault> deadLetterQueue) {
    int count = 0;
    for (Fault fault : deadLetterQueue) {
      if (fault.getType() == type) {
        count++;
      }
    }
    return count;
  }
  
  static Executor oneTimeExecutor(String threadName) {
    return r -> Threads.asyncDaemon(r, threadName);
  }
  
  static <I, O> EgressBuilder<I, O> egressMode(EgressBuilder<I, O> builder, boolean parallel) {
    if (parallel) builder.parallel(); else builder.serial();
    return builder;
  }
}
