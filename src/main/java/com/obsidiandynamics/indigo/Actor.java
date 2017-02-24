package com.obsidiandynamics.indigo;

public abstract class Actor {
  protected void activated(Activation a) {}
  
  protected void passivated(Activation a) {}
  
  protected abstract void act(Activation a);
}
