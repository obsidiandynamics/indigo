package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.*;
import java.util.function.*;

interface TestSupport {
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
}
