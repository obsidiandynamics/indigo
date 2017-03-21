package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatefulLambdaActor<S> implements Actor {
  private final BiConsumer<Activation, S> onAct;
  private final Function<Activation, S> onActivated;
  private final BiConsumer<Activation, S> onPassivated;
  private S state;
  
  StatefulLambdaActor(BiConsumer<Activation, S> onAct, 
                      Function<Activation, S> onActivated, 
                      BiConsumer<Activation, S> onPassivated) {
    this.onAct = onAct;
    this.onActivated = onActivated;
    this.onPassivated = onPassivated;
  }

  @Override
  public void act(Activation a) {
    onAct.accept(a, state);
  }
  
  @Override
  public void activated(Activation a) {
    state = onActivated.apply(a);
  }
  
  @Override
  public void passivated(Activation a) {
    if (onPassivated != null) onPassivated.accept(a, state);
  }
  
  public static final class Builder<S> implements Supplier<Actor> {
    private BiConsumer<Activation, S> onAct;
    private Function<Activation, S> onActivated;
    private BiConsumer<Activation, S> onPassivated;
    
    public Builder<S> act(BiConsumer<Activation, S> onAct) {
      this.onAct = onAct;
      return this;
    }
    
    public Builder<S> activated(Function<Activation, S> onActivated) {
      this.onActivated = onActivated;
      return this;
    }
    
    public Builder<S> passivated(BiConsumer<Activation, S> onPassivated) {
      this.onPassivated = onPassivated;
      return this;
    }
    
    public StatefulLambdaActor<S> build() {
      if (onAct == null) throw new IllegalStateException("No act lambda has been assigned");
      if (onActivated == null) throw new IllegalStateException("No activated lambda has been assigned");
      return new StatefulLambdaActor<>(onAct, onActivated, onPassivated);
    }

    @Override
    public StatefulLambdaActor<S> get() {
      return build();
    }
  }
  
  public static <S> Builder<S> builder() {
    return new Builder<>();
  }
  
  public static <S> void agent(Activation a, S s) {
    a.message().<BiConsumer<Activation, S>>body().accept(a, s);
  }
}
