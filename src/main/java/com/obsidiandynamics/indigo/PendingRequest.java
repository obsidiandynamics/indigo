package com.obsidiandynamics.indigo;

import java.util.function.*;

final class PendingRequest {
  private final Consumer<Activation> onResponse;
  
  private final Consumer<Activation> onTimeout;
  
  private volatile boolean complete;
  
  PendingRequest(Consumer<Activation> onResponse, Consumer<Activation> onTimeout) {
    this.onResponse = onResponse;
    this.onTimeout = onTimeout;
  }

  Consumer<Activation> getOnResponse() {
    return onResponse;
  }

  Consumer<Activation> getOnTimeout() {
    return onTimeout;
  }

  boolean isComplete() {
    return complete;
  }
  
  void setComplete(boolean complete) {
    this.complete = complete;
  }
}
