package com.obsidiandynamics.indigo.marketstrategy.sync;

import java.util.function.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.marketstrategy.*;
import com.obsidiandynamics.indigo.messagebus.*;

public final class StrategyActor implements Actor {
  private final Supplier<Strategy> strategyFactory;
  
  private final MessageBus bus;
  
  private Strategy strategy;
  
  private MessagePublisher publisher;
  
  public StrategyActor(Supplier<Strategy> strategyFactory, MessageBus bus) {
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
      final Order order = strategy.onBar(bar);
      publisher.send(order);
    })
    .otherwise(a::messageFault);
  }
}
