package com.obsidiandynamics.indigo;

public interface Actor {
  default void activated(Activation a) {}
  
  default void passivated(Activation a) {}
  
  void act(Activation a);
}
