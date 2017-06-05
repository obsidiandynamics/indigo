package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;

public final class SubscribeFrame extends IdFrame implements TextEncodedFrame {
  private final String remoteId;
  
  private final String[] topics;
  
  private final Object context;

  public SubscribeFrame(UUID id, String remoteId, String[] topics, Object context) {
    super(id);
    this.remoteId = remoteId;
    this.topics = topics;
    this.context = context;
  }

  @Override
  public FrameType getType() {
    return FrameType.SUBSCRIBE;
  }
  
  public String getRemoteId() {
    return remoteId;
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
    int result = super.hashCode();
    result = prime * result + ((context == null) ? 0 : context.hashCode());
    result = prime * result + ((remoteId == null) ? 0 : remoteId.hashCode());
    result = prime * result + Arrays.hashCode(topics);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    SubscribeFrame other = (SubscribeFrame) obj;
    if (context == null) {
      if (other.context != null)
        return false;
    } else if (!context.equals(other.context))
      return false;
    if (remoteId == null) {
      if (other.remoteId != null)
        return false;
    } else if (!remoteId.equals(other.remoteId))
      return false;
    if (!Arrays.equals(topics, other.topics))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "SubscribeFrame [remoteId=" + remoteId + ", topics=" + Arrays.toString(topics) + ", context=" + context
           + "]";
  }
}
