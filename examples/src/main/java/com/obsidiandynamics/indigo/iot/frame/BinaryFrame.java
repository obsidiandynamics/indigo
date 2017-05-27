package com.obsidiandynamics.indigo.iot.frame;

import java.nio.*;

public final class BinaryFrame implements BinaryEncodedFrame {
  private final ByteBuffer payload;

  public BinaryFrame(ByteBuffer payload) {
    this.payload = payload;
  }

  @Override
  public FrameType getType() {
    return FrameType.RECEIVE;
  }

  public final ByteBuffer getPayload() {
    return payload;
  }
  
  @Override
  public String toString() {
    return "Binary [payload=" + payload + "]";
  }
}
