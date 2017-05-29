package com.obsidiandynamics.indigo.iot.frame;

public final class TextFrame implements TextEncodedFrame {
  private final String topic;
  
  private final String payload;

  public TextFrame(String topic, String payload) {
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

  public final String getPayload() {
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
    TextFrame other = (TextFrame) obj;
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
    return "Text [topic=" + topic + ", payload=" + payload + "]";
  }
}
