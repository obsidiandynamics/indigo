package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;

public final class SubscribeFrame extends IdFrame {
  private final String[] topics;
  
  private final Object context;

  public SubscribeFrame(UUID id, String[] topics, Object context) {
    super(id);
    this.topics = topics;
    this.context = context;
  }

  @Override
  protected FrameType getType() {
    return FrameType.SUBSCRIBE;
  }

  public final String[] getTopics() {
    return topics;
  }

  public final Object getContext() {
    return context;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((context == null) ? 0 : context.hashCode());
    result = prime * result + Arrays.hashCode(topics);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SubscribeFrame other = (SubscribeFrame) obj;
    if (context == null) {
      if (other.context != null)
        return false;
    } else if (!context.equals(other.context))
      return false;
    if (!Arrays.equals(topics, other.topics))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "SubscribeFrame [id=" + getId() + ", topics=" + Arrays.toString(topics) + ", context=" + context + "]";
  }
}
