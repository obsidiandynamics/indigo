package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

final class Stash {
  Predicate<Message> filter;
  
  final List<Message> messages = new ArrayList<>();
  
  boolean unstashing;
}
