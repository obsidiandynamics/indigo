package com.obsidiandynamics.indigo.marketstrategy;

import com.obsidiandynamics.indigo.*;

public final class RouterActor implements Actor {
  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Bar.class).then(bar -> {
      a.forward(m).to(ActorRef.of("strategy", bar.getSymbol()));
    })
    .otherwise(a::messageFault);
  }
}
