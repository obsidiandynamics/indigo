package com.obsidiandynamics.indigo;

public final class ActorId {
  private final Object type;
  
  private final Object key;

  public ActorId(Object type, Object key) {
    this.type = type;
    this.key = key;
  }

  public Object getType() {
    return type;
  }

  public Object getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "ActorId [type=" + type + ", key=" + key + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    ActorId other = (ActorId) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }
}
