package com.obsidiandynamics.indigo;

@FunctionalInterface
public interface ActivationFactory {
  Activation create(long id, ActorRef ref, ActorSystem system, ActorConfig actorConfig, Actor actor);
}
