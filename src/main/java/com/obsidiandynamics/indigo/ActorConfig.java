package com.obsidiandynamics.indigo;

public abstract class ActorConfig {
  protected int priority = PropertyUtils.get("indigo.priority", Integer::parseInt, 1);
}
