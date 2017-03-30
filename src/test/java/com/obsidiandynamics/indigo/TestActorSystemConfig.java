package com.obsidiandynamics.indigo;

public abstract class TestActorSystemConfig extends ActorSystemConfig {{
  exceptionHandler = (sys, t) -> {
    sys.addError(t);
    t.printStackTrace();
  };
}}
