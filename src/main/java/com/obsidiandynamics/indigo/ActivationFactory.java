package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

@FunctionalInterface
public interface ActivationFactory {
  Activation create(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor, Executor executor);
}
