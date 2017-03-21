package com.obsidiandynamics.indigo;

import com.obsidiandynamics.indigo.util.*;

public abstract class ActorConfig {
  public int priority = PropertyUtils.get("indigo.priority", Integer::parseInt, 1);

  public boolean throttleSend = PropertyUtils.get("indigo.throttleSend", Boolean::parseBoolean, false);
}
