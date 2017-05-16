package com.obsidiandynamics.indigo.marketfeed;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.db.*;

public final class FeedActor implements Actor {
  private final FeedActorConfig config;
  
  private FeedActorState state;
  
  public FeedActor(FeedActorConfig config) {
    this.config = config;
  }
  
  @Override
  public void activated(Activation a) {
    state = new FeedActorState();
    //TODO
  }
  
  @Override
  public void passivated(Activation a) {
    //TODO
  }
  
  @Override
  public void act(Activation a, Message m) {
    // TODO Auto-generated method stub
  }
}
