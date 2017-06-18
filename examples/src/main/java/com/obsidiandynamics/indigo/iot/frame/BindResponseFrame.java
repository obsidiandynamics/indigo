package com.obsidiandynamics.indigo.iot.frame;

import java.util.*;import com.obsidiandynamics.indigo.iot.frame.Error;

public final class BindResponseFrame extends IdFrame implements TextEncodedFrame {
  public static String JSON_TYPE_NAME = "BindResponse";
  
  private final Error[] errors;
  
  public BindResponseFrame(UUID messageId, Collection<? extends Error> errors) {
    this(messageId, errors.toArray(new Error[errors.size()]));
  }

  public BindResponseFrame(UUID messageId, Error ... errors) {
    super(messageId);
    this.errors = errors;
  }

  @Override
  public FrameType getType() {
    return FrameType.BIND;
  }

  public final boolean isSuccess() {
    return errors.length == 0;
  }

  public final Error[] getErrors() {
    return errors;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.hashCode(errors);
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
    if (!Arrays.equals(errors, other.errors))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "BindResponse [messageId=" + getMessageId() + ", errors=" + Arrays.toString(errors) + "]";
  }
}
