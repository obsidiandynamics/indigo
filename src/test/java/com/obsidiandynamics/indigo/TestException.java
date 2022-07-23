package com.obsidiandynamics.indigo;

import java.util.function.*;

final class TestException extends RuntimeException {
  static final BiConsumer<ActorSystem, Throwable> BYPASS_DRAIN_HANDLER = (sys, t) -> {
    if (! (t instanceof TestException)) {
      sys.addError(t);
      t.printStackTrace();
    }
  };
  
  private static final long serialVersionUID = 1L;
  
  TestException(String m) { super(m); }
}