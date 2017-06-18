package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;

public abstract class IdFrame implements Frame {
  private UUID messageId;
  
  protected IdFrame(UUID messageId) {
    this.messageId = messageId;
  }

  public final UUID getMessageId() {
    return messageId;
  }
  
  protected void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
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
    if (messageId == null) {
      if (other.messageId != null)
        return false;
    } else if (!messageId.equals(other.messageId))
      return false;
    return true;
  }
  
  @Override
  public abstract String toString();
}
