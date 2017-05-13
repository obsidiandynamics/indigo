package com.obsidiandynamics.indigo;

import java.util.function.*;

final class BackgroundAgent implements Actor {
  @Override
  public void act(Activation a, Message m) {
    m.<Consumer<Activation>>body().accept(a);
    a.passivate();
  }
}
