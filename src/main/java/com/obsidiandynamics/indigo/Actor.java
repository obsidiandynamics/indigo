package com.obsidiandynamics.indigo;

@FunctionalInterface
public interface Actor {
  default void activated(Activation a) {}
  
  default void passivated(Activation a) {}
  
  void act(Activation a, Message m);
}
