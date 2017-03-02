package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatelessLambdaActor extends Actor {
  private final Consumer<Activation> onAct;
  private final Consumer<Activation> onActivated;
  private final Consumer<Activation> onPassivated;
  
  private StatelessLambdaActor(Consumer<Activation> onAct, 
                               Consumer<Activation> onActivated, 
                               Consumer<Activation> onPassivated) {
    this.onAct = onAct;
    this.onActivated = onActivated;
    this.onPassivated = onPassivated;
  }

  @Override
  protected void act(Activation a) {
    onAct.accept(a);
  }
  
  @Override
  protected void activated(Activation a) {
    if (onActivated != null) onActivated.accept(a);
  }
  
  @Override
  protected void passivated(Activation a) {
    if (onPassivated != null) onPassivated.accept(a);
  }
  
  public static final class Builder implements Supplier<Actor> {
    private Consumer<Activation> onAct;
    private Consumer<Activation> onActivated;
    private Consumer<Activation> onPassivated;
    
    public Builder act(Consumer<Activation> onAct) {
      this.onAct = onAct;
      return this;
    }
    
    public Builder activated(Consumer<Activation> onActivated) {
      this.onActivated = onActivated;
      return this;
    }
    
    public Builder passivated(Consumer<Activation> onPassivated) {
      this.onPassivated = onPassivated;
      return this;
    }
    
    public StatelessLambdaActor build() {
      if (onAct == null) throw new IllegalStateException("No act lambda has been assigned");
      return new StatelessLambdaActor(onAct, onActivated, onPassivated);
    }

    @Override
    public StatelessLambdaActor get() {
      return build();
    }
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  public static void agent(Activation a) {
    a.message().<Consumer<Activation>>body().accept(a);
  }
}
