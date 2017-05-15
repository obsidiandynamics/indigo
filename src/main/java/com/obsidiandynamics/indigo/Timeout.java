package com.obsidiandynamics.indigo;

final class Timeout implements Signal {
  private static final Timeout INSTANCE = new Timeout();
  
  private Timeout() {}
  
  static Timeout instance() { return INSTANCE; }
}
