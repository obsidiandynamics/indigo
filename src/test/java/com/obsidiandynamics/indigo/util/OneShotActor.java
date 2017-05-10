package com.obsidiandynamics.indigo.util;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.OneShotContract.*;

public final class OneShotActor implements Actor {
  private final Operation operation;
  
  private boolean started;
  
  private boolean finished;
  
  private Object result;
  
  public OneShotActor(Operation operation) {
    this.operation = operation;
  }

  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Fire.class).then(b -> fire(a, m))
    .when(GetStatus.class).then(b -> getStatus(a, m))
    .otherwise(a::messageFault);
  }
  
  private void fire(Activation a, Message m) {
    final boolean fired = ! started;
    if (fired) {
      final Fire fire = m.body();
      started = true;
      final CompletableFuture<?> future = operation.operate(a, fire.body);
      future.thenAccept(value -> {
        finished = true;
        result = value;
      });
    }
    a.reply(m).tell(new FireResponse(fired, toStatus()));
  }
  
  private void getStatus(Activation a, Message m) {
    a.reply(m).tell(new GetStatusResponse(toStatus()));
  }
  
  private OneShotStatus toStatus() {
    return new OneShotStatus(started, finished, result);
  }
}
