package com.obsidiandynamics.indigo.adder;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.AdderContract.*;

public final class AdderActor implements Actor {
  /** The current sum. */
  private int sum;
  
  @Override
  public void act(Activation a, Message m) {
    if (m.body() instanceof Add) {
      sum += m.<Add>body().getValue();
    } else if (m.body() instanceof Get) {
      a.reply(m).tell(new GetResponse(sum));
    }
  }
}
