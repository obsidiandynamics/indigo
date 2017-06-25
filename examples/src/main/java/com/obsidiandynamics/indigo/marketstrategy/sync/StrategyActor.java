package com.obsidiandynamics.indigo.marketstrategy.sync;

import java.util.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.marketstrategy.*;

public final class StrategyActor implements Actor {
  private final Supplier<Strategy> strategyFactory;
  
  private final Supplier<OrderHandler> orderHandlerFactory;
  
  private Strategy strategy;
  
  private OrderHandler orderHandler;
  
  public StrategyActor(Supplier<Strategy> strategyFactory, Supplier<OrderHandler> orderHandlerFactory) {
    this.strategyFactory = strategyFactory;
    this.orderHandlerFactory = orderHandlerFactory;
  }

  @Override
  public void activated(Activation a) {
    strategy = strategyFactory.get();
    orderHandler = orderHandlerFactory.get();
  }
  
  @Override
  public void passivated(Activation a) {
    orderHandler.close();
  }

  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Bar.class).then(bar -> {
      Optional.ofNullable(strategy.onBar(bar)).ifPresent(orderHandler::accept);
    })
    .otherwise(a::messageFault);
  }
}
