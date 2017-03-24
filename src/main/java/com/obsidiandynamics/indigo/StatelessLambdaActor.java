package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatelessLambdaActor implements Actor {
  private final BiConsumer<Activation, Message> onAct;
  private final Consumer<Activation> onActivated;
  private final Consumer<Activation> onPassivated;
  
  private StatelessLambdaActor(BiConsumer<Activation, Message> onAct, 
                               Consumer<Activation> onActivated, 
                               Consumer<Activation> onPassivated) {
    this.onAct = onAct;
    this.onActivated = onActivated;
    this.onPassivated = onPassivated;
  }

  @Override
  public void act(Activation a, Message m) {
    onAct.accept(a, m);
  }
  
  @Override
  public void activated(Activation a) {
    if (onActivated != null) onActivated.accept(a);
  }
  
  @Override
  public void passivated(Activation a) {
    if (onPassivated != null) onPassivated.accept(a);
  }
  
  public static final class Builder implements Supplier<Actor> {
    private BiConsumer<Activation, Message> onAct;
    private Consumer<Activation> onActivated;
    private Consumer<Activation> onPassivated;
    
    public Builder act(BiConsumer<Activation, Message> onAct) {
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
  
  public static void agent(Activation a, Message m) {
    m.<Consumer<Activation>>body().accept(a);
  }
}
