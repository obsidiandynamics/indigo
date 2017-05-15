package com.obsidiandynamics.indigo;

final class SleepingPill implements Signal {
  private static final SleepingPill INSTANCE = new SleepingPill();
  
  private SleepingPill() {}
  
  static SleepingPill instance() { return INSTANCE; }
}
