package com.obsidiandynamics.indigo.iot.frame;

public final class TopicAccessError extends Error {
  public static String JSON_TYPE_NAME = "TopicAccess";
  
  private final String topic;
  
  public TopicAccessError(String description, String topic) {
    super(description);
    this.topic = topic;
  }
  
  public String getTopic() {
    return topic;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((topic == null) ? 0 : topic.hashCode());
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
    TopicAccessError other = (TopicAccessError) obj;
    if (topic == null) {
      if (other.topic != null)
        return false;
    } else if (!topic.equals(other.topic))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "TopicAccessError [topic=" + topic + ", description=" + getDescription() + "]";
  }
}
