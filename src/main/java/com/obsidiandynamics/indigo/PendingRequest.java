package com.obsidiandynamics.indigo;

import java.util.function.*;

final class PendingRequest {
  private final Consumer<Message> onResponse;
  
  private final Runnable onTimeout;
  
  private final Consumer<Fault> onFault;
  
  private TimeoutTask timeoutTask;
  
  private volatile boolean complete;
  
  PendingRequest(Consumer<Message> onResponse, Runnable onTimeout, Consumer<Fault> onFault) {
    this.onResponse = onResponse;
    this.onTimeout = onTimeout;
    this.onFault = onFault;
  }

  Consumer<Message> getOnResponse() {
    return onResponse;
  }

  Runnable getOnTimeout() {
    return onTimeout;
  }
  
  Consumer<Fault> getOnFault() {
    return onFault;
  }

  boolean isComplete() {
    return complete;
  }
  
  void setComplete(boolean complete) {
    this.complete = complete;
  }

  TimeoutTask getTimeoutTask() {
    return timeoutTask;
  }

  void setTimeoutTask(TimeoutTask timeoutTask) {
    this.timeoutTask = timeoutTask;
  }
}
