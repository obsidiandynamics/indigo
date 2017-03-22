package com.obsidiandynamics.indigo;

import com.obsidiandynamics.indigo.util.*;

public abstract class ActorConfig {
  /** The number of consecutive turns an actor is accorded before releasing its thread. */
  public int bias = PropertyUtils.get("indigo.bias", Integer::parseInt, 1);
  
  /** The backlog level at which point throttling is enforced. Set to <code>Integer.MAX_VALUE</code> to
   *  avoid throttling. */
  public long backlogThrottleCapacity = PropertyUtils.get("indigo.backlogThrottleCapacity", Long::parseLong, 10_000L);
  
  /** The time penalty for each throttling block. */
  public int backlogThrottleMillis = PropertyUtils.get("indigo.backlogThrottleMillis", Integer::parseInt, 1);
  
  /** Upper bound on the number of consecutive penalties imposed during throttling, after which the message
   *  will be enqueued even if the backlog is over capacity. */
  public int backlogThrottleTries = PropertyUtils.get("indigo.backlogThrottleTries", Integer::parseInt, 10);
}
