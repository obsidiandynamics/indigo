package com.obsidiandynamics.indigo.iot.rig;

final class Sync extends RigSubframe {
  private final long nanoTime;

  Sync(long nanoTime) {
    this.nanoTime = nanoTime;
  }
  
  long getNanoTime() {
    return nanoTime;
  }

  @Override
  public String toString() {
    return "Sync [nanoTime=" + nanoTime + "]";
  }
}
