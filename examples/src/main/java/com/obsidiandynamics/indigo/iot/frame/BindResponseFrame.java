package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;

public final class BindResponseFrame extends IdFrame implements TextEncodedFrame {
  private final Object error;

  public BindResponseFrame(UUID messageId, Object error) {
    super(messageId);
    this.error = error;
  }

  @Override
  public FrameType getType() {
    return FrameType.BIND;
  }

  public final boolean isSuccess() {
    return error == null;
  }

  public final Object getError() {
    return error;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((error == null) ? 0 : error.hashCode());
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
    BindResponseFrame other = (BindResponseFrame) obj;
    if (error == null) {
      if (other.error != null)
        return false;
    } else if (!error.equals(other.error))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "BindResponse [messageId=" + getMessageId() + ", error=" + String.valueOf(error) + "]";
  }
}
