package com.obsidiandynamics.indigo;

import com.obsidiandynamics.indigo.util.*;

public abstract class ActorConfig {
  public int bias = PropertyUtils.get("indigo.bias", Integer::parseInt, 1);

  public boolean throttleSend = PropertyUtils.get("indigo.throttleSend", Boolean::parseBoolean, false);
}
