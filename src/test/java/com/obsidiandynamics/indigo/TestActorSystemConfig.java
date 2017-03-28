package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;

public abstract class TestActorSystemConfig extends ActorSystemConfig {{
  exceptionHandler = DRAIN;
}}
