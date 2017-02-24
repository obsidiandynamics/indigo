package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatelessLambdaActor extends Actor {
  private final Consumer<Activation> act;
  private final Consumer<Activation> activated;
  private final Consumer<Activation> passivated;
  
  private StatelessLambdaActor(Consumer<Activation> act, 
                               Consumer<Activation> activated, 
                               Consumer<Activation> passivated) {
    this.act = act;
    this.activated = activated;
    this.passivated = passivated;
  }

  @Override
  protected void act(Activation a) {
    act.accept(a);
  }
  
  @Override
  protected void activated(Activation a) {
    if (activated != null) activated.accept(a);
  }
  
  @Override
  protected void passivated(Activation a) {
    if (passivated != null) passivated.accept(a);
  }
  
  public static final class Builder implements Supplier<Actor> {
    private Consumer<Activation> act;
    private Consumer<Activation> activated;
    private Consumer<Activation> passivated;
    
    public Builder act(Consumer<Activation> act) {
      this.act = act;
      return this;
    }
    
    public Builder activated(Consumer<Activation> activated) {
      this.activated = activated;
      return this;
    }
    
    public Builder passivated(Consumer<Activation> passivated) {
      this.passivated = passivated;
      return this;
    }
    
    public StatelessLambdaActor build() {
      if (act == null) throw new IllegalStateException("No act lambda has been assigned");
      return new StatelessLambdaActor(act, activated, passivated);
    }

    @Override
    public StatelessLambdaActor get() {
      return build();
    }
  }
  
  public static Builder builder() {
    return new Builder();
  }
}
