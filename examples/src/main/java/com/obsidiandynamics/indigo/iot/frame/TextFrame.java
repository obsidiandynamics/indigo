package com.obsidiandynamics.indigo.iot.frame;

public final class TextFrame extends Frame {
  private final String payload;

  public TextFrame(String payload) {
    this.payload = payload;
  }

  @Override
  protected FrameType getType() {
    return FrameType.RECEIVE;
  }

  public final String getPayload() {
    return payload;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((payload == null) ? 0 : payload.hashCode());
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
    TextFrame other = (TextFrame) obj;
    if (payload == null) {
      if (other.payload != null)
        return false;
    } else if (!payload.equals(other.payload))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Text [payload=" + payload + "]";
  }
}
