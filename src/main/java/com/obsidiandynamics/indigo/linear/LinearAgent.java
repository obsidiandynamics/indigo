package com.obsidiandynamics.indigo.linear;

import com.obsidiandynamics.indigo.*;

final class LinearAgent implements Actor {
  static final String ROLE = "agent";
  
  private final ActorExecutor executor;
  
  LinearAgent(ActorExecutor executor) {
    this.executor = executor;
  }

  @Override
  public void activated(Activation a) {
    executor.notifyAgentActivated(a.self().key());
  }

  @Override
  public void passivated(Activation a) {
    executor.notifyAgentPassivated(a.self().key());
  }
  
  @Override
  public void act(Activation a, Message m) {
    final LinearFutureTask<?> linearFutureTask = m.body();
    linearFutureTask.run();
  }
}
