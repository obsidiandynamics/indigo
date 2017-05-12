package com.obsidiandynamics.indigo.adder;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.AdderContract.*;
import com.obsidiandynamics.indigo.adder.db.*;

public final class PersistentSyncAdderActor implements Actor {
  private final AdderDB db;

  /** The current sum. */
  private int sum;
  
  public PersistentSyncAdderActor(AdderDB db) {
    this.db = db;
  }
  
  @Override
  public void activated(Activation a) {
    final SavePoint saved = db.getSavePoint(a.self());
    sum = saved.getSum();
  }
  
  @Override
  public void passivated(Activation a) {
    db.setSavePoint(SavePoint.of(a.self(), sum));
  }
  
  @Override
  public void act(Activation a, Message m) {
    m.select()
    .when(Add.class).then(b -> sum += b.getValue())
    .when(Get.class).then(b -> a.reply(m).tell(new GetResponse(sum)))
    .otherwise(a::messageFault);
  }
}
