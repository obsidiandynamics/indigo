package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;

public abstract class IdFrame implements Frame {
  private UUID messageId;
  
  protected IdFrame(UUID messageId) {
    this.messageId = messageId;
  }
  
  public UUID getMessageId() {
    return messageId != null ? messageId : new UUID(0, 0);
  }
  
  protected void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getMessageId().hashCode();
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
    IdFrame other = (IdFrame) obj;
    if (!getMessageId().equals(other.getMessageId()))
      return false;
    return true;
  }
  
  @Override
  public abstract String toString();
}
