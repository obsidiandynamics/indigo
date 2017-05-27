package com.obsidiandynamics.indigo.iot.frame;

import java.nio.*;

public final class PublishBinaryFrame implements BinaryEncodedFrame {
  private final String topic;
  
  private final ByteBuffer payload;

  public PublishBinaryFrame(String topic, ByteBuffer payload) {
    this.topic = topic;
    this.payload = payload;
  }

  @Override
  public FrameType getType() {
    return FrameType.PUBLISH;
  }

  public final String getTopic() {
    return topic;
  }

  public final ByteBuffer getPayload() {
    return payload;
  }
  
  @Override
  public String toString() {
    return "PublishBinary [topic=" + topic + ", payload.remaining=" + payload.remaining() + "]";
  }
}
