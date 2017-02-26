package com.obsidiandynamics.indigo;

import java.util.function.*;

final class PendingRequest {
  private final Consumer<Activation> onResponse;
  
  private final Consumer<Activation> onTimeout;
  
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
}
