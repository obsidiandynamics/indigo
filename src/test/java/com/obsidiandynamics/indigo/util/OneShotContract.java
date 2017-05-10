package com.obsidiandynamics.indigo.util;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.*;

public final class OneShotContract {
  public final static class Fire {
    final Object body;

    public Fire(Object body) {
      this.body = body;
    }
  }
  
  public final static class FireResponse {
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
  
  public final static class GetStatus {}
  
  public final static class GetStatusResponse {
    private final OneShotStatus status;

    GetStatusResponse(OneShotStatus status) {
      this.status = status;
    }

    public OneShotStatus getStatus() {
      return status;
    }
  }
  
  public final static class OneShotStatus {
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
  
  private OneShotContract() {}
}
