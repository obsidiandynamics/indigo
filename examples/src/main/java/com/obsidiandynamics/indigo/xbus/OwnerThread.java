package com.obsidiandynamics.indigo.xbus;

public final class OwnerThread {
  private final Thread owner = Thread.currentThread();
  
  public boolean isCurrent() {
    return Thread.currentThread() == owner;
  }
  
  public void verifyCurrent() {
    if (! isCurrent()) {
      throw new IllegalStateException("Can only be invoked by " + owner + ", not by " + Thread.currentThread());
    }
  }

  @Override
  public String toString() {
    return "OwnerThread [owner=" + owner + "]";
  }
}
