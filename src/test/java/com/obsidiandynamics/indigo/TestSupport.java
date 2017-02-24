package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

interface TestSupport {
  default Consumer<Activation> refCollector(Set<ActorRef> set) {
    return a -> set.add(a.message().from());
  }
}
