package com.obsidiandynamics.indigo.iot.frame;

import java.nio.*;

public final class BinaryFrame implements BinaryEncodedFrame {
  private final String topic;
  
  private final ByteBuffer payload;

  public BinaryFrame(String topic, ByteBuffer payload) {
    this.topic = topic;
    this.payload = payload;
  }

  @Override
  public FrameType getType() {
    return FrameType.RECEIVE;
  }

  public final String getTopic() {
    return topic;
  }

  public final ByteBuffer getPayload() {
    return payload;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((payload == null) ? 0 : payload.hashCode());
    result = prime * result + ((topic == null) ? 0 : topic.hashCode());
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
    BinaryFrame other = (BinaryFrame) obj;
    if (payload == null) {
      if (other.payload != null)
        return false;
    } else if (!payload.equals(other.payload))
      return false;
    if (topic == null) {
      if (other.topic != null)
        return false;
    } else if (!topic.equals(other.topic))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Binary [topic=" + topic + ", payload.remaining=" + payload.remaining() + "]";
  }
}
