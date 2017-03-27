package com.obsidiandynamics.indigo;

import java.util.*;
import java.util.function.*;

final class Stash {
  final Predicate<Message> filter;
  
  final List<Message> messages = new ArrayList<>();
  
  Stash(Predicate<Message> filter) {
    this.filter = filter;
  }
}
