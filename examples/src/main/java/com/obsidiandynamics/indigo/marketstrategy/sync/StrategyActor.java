package com.obsidiandynamics.indigo.marketstrategy.sync;

import java.util.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.marketstrategy.*;
import com.obsidiandynamics.indigo.xbus.*;

public final class StrategyActor implements Actor {
  private final Supplier<Strategy> strategyFactory;
  
  private final XBus bus;
  
  private Strategy strategy;
  
  private XPublisher publisher;
  
  public StrategyActor(Supplier<Strategy> strategyFactory, XBus bus) {
    this.strategyFactory = strategyFactory;
    this.bus = bus;
  }

  @Override
  public void activated(Activation a) {
    strategy = strategyFactory.get();
    publisher = bus.getPublisher("orders");
  }
  
  @Override
  public void passivated(Activation a) {
    publisher.close();
  }

  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Bar.class).then(bar -> {
      Optional.ofNullable(strategy.onBar(bar)).ifPresent(publisher::send);
    })
    .otherwise(a::messageFault);
  }
}
