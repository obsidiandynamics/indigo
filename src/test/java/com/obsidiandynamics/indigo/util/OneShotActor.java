package com.obsidiandynamics.indigo.util;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class OneShotActor implements Actor {
  public static class Fire {
    final Object body;

    public Fire(Object body) {
      this.body = body;
    }
  }
  
  public static class FireResponse {
    private final boolean fired;
    private final OneShotStatus status;

    FireResponse(boolean fired, OneShotStatus status) {
      this.fired = fired;
      this.status = status;
    }
    
    public boolean isFired() {
      return fired;
    }

    public OneShotStatus getStatus() {
      return status;
    }
  }
  
  public static class GetStatus {}
  
  public static class GetStatusResponse {
    private final OneShotStatus status;

    GetStatusResponse(OneShotStatus status) {
      this.status = status;
    }

    public OneShotStatus getStatus() {
      return status;
    }
  }
  
  public static class OneShotStatus {
    private final boolean started;
    
    private final boolean finished;
    
    private final Object result;

    OneShotStatus(boolean started, boolean finished, Object result) {
      this.started = started;
      this.finished = finished;
      this.result = result;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isFinished() {
      return finished;
    }

    public Object getResult() {
      return result;
    }

    @Override
    public String toString() {
      return "OneShotStatus [started=" + started + ", finished=" + finished + ", result=" + result + "]";
    }
  }
  
  @FunctionalInterface
  public interface Operation {
    CompletableFuture<?> operate(Activation a, Object requestBody);
  }
  
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
