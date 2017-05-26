package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;

public final class SubscribeResponseFrame extends IdFrame {
  private final Object error;

  public SubscribeResponseFrame(UUID id, Object error) {
    super(id);
    this.error = error;
  }

  @Override
  protected FrameType getType() {
    return FrameType.SUBSCRIBE;
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
    SubscribeResponseFrame other = (SubscribeResponseFrame) obj;
    if (error == null) {
      if (other.error != null)
        return false;
    } else if (!error.equals(other.error))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "SubscribeResponseFrame [id=" + getId() + ", error=" + String.valueOf(error) + "]";
  }
}
