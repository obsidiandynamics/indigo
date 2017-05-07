package com.obsidiandynamics.indigo.adder;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.AdderContract.*;

public final class AdderActor implements Actor {
  /** The current sum. */
  private int sum;
  
  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Add.class).then(b -> sum += b.getValue())
    .when(Get.class).then(b -> a.reply(m).tell(new GetResponse(sum)))
    .otherwise(a::messageFault);
  }
}
