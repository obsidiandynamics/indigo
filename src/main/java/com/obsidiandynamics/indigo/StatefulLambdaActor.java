package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatefulLambdaActor<S> extends Actor {
  private final BiConsumer<Activation, S> act;
  private final Function<Activation, S> activated;
  private final BiConsumer<Activation, S> passivated;
  private S state;
  
  StatefulLambdaActor(BiConsumer<Activation, S> act, 
                      Function<Activation, S> activated, 
                      BiConsumer<Activation, S> passivated) {
    this.act = act;
    this.activated = activated;
    this.passivated = passivated;
  }

  @Override
  protected void act(Activation a) {
    act.accept(a, state);
  }
  
  @Override
  protected void activated(Activation a) {
    state = activated.apply(a);
  }
  
  @Override
  protected void passivated(Activation a) {
    if (passivated != null) passivated.accept(a, state);
  }
  
  public static final class Builder<S> implements Supplier<Actor> {
    private BiConsumer<Activation, S> act;
    private Function<Activation, S> activated;
    private BiConsumer<Activation, S> passivated;
    
    public Builder<S> act(BiConsumer<Activation, S> act) {
      this.act = act;
      return this;
    }
    
    public Builder<S> activated(Function<Activation, S> activated) {
      this.activated = activated;
      return this;
    }
    
    public Builder<S> passivated(BiConsumer<Activation, S> passivated) {
      this.passivated = passivated;
      return this;
    }
    
    public StatefulLambdaActor<S> build() {
      if (act == null) throw new IllegalStateException("No act lambda has been assigned");
      if (activated == null) throw new IllegalStateException("No activated lambda has been assigned");
      return new StatefulLambdaActor<>(act, activated, passivated);
    }

    @Override
    public StatefulLambdaActor<S> get() {
      return build();
    }
  }
  
  public static <S> Builder<S> builder() {
    return new Builder<>();
  }
}
