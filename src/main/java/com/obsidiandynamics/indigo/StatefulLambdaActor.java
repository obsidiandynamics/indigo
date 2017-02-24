package com.obsidiandynamics.indigo;

import java.util.function.*;

public final class StatefulLambdaActor<S> extends Actor {
  private final BiConsumer<Activation, S> act;
  private final S state;
  private final Consumer<Activation> activated;
  private final Consumer<Activation> passivated;
  
  StatefulLambdaActor(BiConsumer<Activation, S> act, 
                      S state,
                      Consumer<Activation> activated, 
                      Consumer<Activation> passivated) {
    this.act = act;
    this.state = state;
    this.activated = activated;
    this.passivated = passivated;
  }

  @Override
  protected void act(Activation a) {
    act.accept(a, state);
  }
  
  @Override
  protected void activated(Activation a) {
    if (activated != null) activated.accept(a);
  }
  
  @Override
  protected void passivated(Activation a) {
    if (passivated != null) passivated.accept(a);
  }
  
  public static final class Builder<S> implements Supplier<Actor> {
    private Supplier<S> stateFactory;
    private BiConsumer<Activation, S> act;
    private Consumer<Activation> activated;
    private Consumer<Activation> passivated;
    
    public Builder<S> stateFactory(Supplier<S> stateFactory) {
      this.stateFactory = stateFactory;
      return this;
    }
    
    public Builder<S> act(BiConsumer<Activation, S> act) {
      this.act = act;
      return this;
    }
    
    public Builder<S> activated(Consumer<Activation> activated) {
      this.activated = activated;
      return this;
    }
    
    public Builder<S> passivated(Consumer<Activation> passivated) {
      this.passivated = passivated;
      return this;
    }
    
    public StatefulLambdaActor<S> build(S state) {
      if (act == null) throw new IllegalStateException("No act lambda has been assigned");
      if (stateFactory == null) throw new IllegalStateException("No state factory has been assigned");
      return new StatefulLambdaActor<>(act, state, activated, passivated);
    }

    @Override
    public StatefulLambdaActor<S> get() {
      return build(stateFactory.get());
    }
  }
  
  public static <S> Builder<S> builder() {
    return new Builder<>();
  }
}
