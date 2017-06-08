package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;

@FunctionalInterface
interface Subscription {
  Collection<String> getTopics();
}
