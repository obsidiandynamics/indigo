package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;

@FunctionalInterface
interface Subscription {
  Set<String> getTopics();
}
