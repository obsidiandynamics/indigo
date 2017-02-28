package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.*;
import java.util.function.*;

interface TestSupport {
  static final boolean LOG = false;
  static final PrintStream LOG_STREAM = System.out;
  
  default void logTestName() {
    if (LOG) LOG_STREAM.println("Testing " + getClass().getSimpleName());
  }
  
  default Consumer<Activation> refCollector(Set<ActorRef> set) {
    return a -> set.add(a.message().from());
  }
  
  default Consumer<Activation> tell(String role) {
    return a -> a.to(ActorRef.of(role)).tell();
  }
}
