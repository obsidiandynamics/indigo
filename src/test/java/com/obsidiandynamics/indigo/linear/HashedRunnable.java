package com.obsidiandynamics.indigo.linear;

final class HashedRunnable implements Runnable {
  private final Runnable delegate;
  
  private final int hashCode;
  
  HashedRunnable(Runnable delegate, int hashCode) {
    this.delegate = delegate;
    this.hashCode = hashCode;
  }
  
  @Override
  public void run() {
    delegate.run();
  }
  
  @Override
  public int hashCode() {
    return hashCode;
  }
}
