package com.obsidiandynamics.indigo;

import java.util.concurrent.*;
import java.util.function.*;

public final class ActorBuilder {
  private final ActorSystem system;
  private final String role;
  private ActorConfig actorConfig;
  
  ActorBuilder(ActorSystem system, String role) {
    this.system = system;
    this.role = role;
    actorConfig = system.getConfig().defaultActorConfig;
  }
  
  public ActorBuilder withConfig(ActorConfig actorConfig) {
    this.actorConfig = actorConfig;
    return this;
  }
  
  public ActorSystem cue(BiConsumer<Activation, Message> act) {
    return cue(StatelessLambdaActor.builder().act(act)); 
  }
  
  public <S> ActorSystem cue(Supplier<S> stateFactory, TriConsumer<Activation, Message, S> act) {
    return cue(StatefulLambdaActor.<S>builder().act(act).activated(a -> CompletableFuture.completedFuture(stateFactory.get())));
  }
  
  public <S> ActorSystem cue(Function<Activation, S> stateFactory, TriConsumer<Activation, Message, S> act) {
    return cue(StatefulLambdaActor.<S>builder().act(act).activated(a -> CompletableFuture.completedFuture(stateFactory.apply(a))));
  }
  
  public <S> ActorSystem cueAsync(Function<Activation, CompletableFuture<S>> futureStateFactory, TriConsumer<Activation, Message, S> act) {
    return cue(StatefulLambdaActor.<S>builder().act(act).activated(futureStateFactory));
  }
  
  public ActorSystem cue(Supplier<? extends Actor> factory) {
    system.registerActor(role, factory, actorConfig);
    return system;
  }
}