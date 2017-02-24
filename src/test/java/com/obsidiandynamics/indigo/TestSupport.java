package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

interface TestSupport {
  default Consumer<Activation> refCollector(Set<ActorRef> set) {
    return a -> set.add(a.message().from());
  }
  
  default Consumer<Activation> tell(String role) {
    return a -> a.to(ActorRef.of(role)).tell();
  }
}
