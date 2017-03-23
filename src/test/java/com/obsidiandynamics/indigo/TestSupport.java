package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public interface TestSupport {
  static final boolean LOG = false;
  static final PrintStream LOG_STREAM = System.out;
  
  default void logTestName() {
    log("Testing %s\n", getClass().getSimpleName());
  }
  
  default void log(String format, Object ... args) {
    if (LOG) LOG_STREAM.printf(format, args);
  }
  
  default Consumer<Activation> refCollector(Set<ActorRef> set) {
    return a -> {
      log("Finished %s\n", a.message().from());
      set.add(a.message().from());
    };
  }
  
  default Consumer<Activation> tell(String role) {
    return a -> a.to(ActorRef.of(role)).tell();
  }
  
  static long took(Runnable r) {
    final long started = System.nanoTime();
    r.run();
    final long took = System.nanoTime() - started;
    return took / 1_000_000l;
  }
  
  static void parallel(int threads, CountDownLatch latch, Consumer<Integer> r) {
    for (int i = 0; i < threads; i++) {
      final int _i = i;
      new Thread(() -> r.accept(_i)).start();
    }
    Threads.await(latch);
  }
}
