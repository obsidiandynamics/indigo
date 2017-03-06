package com.obsidiandynamics.indigo;

public final class ActorRef {
  public static final String INGRESS = "_ingress";
  
  private final String role;
  
  private final String key;

  private ActorRef(String role, String key) {
    this.role = role;
    this.key = key;
  }

  public String role() {
    return role;
  }

  public String key() {
    return key;
  }
  
  public boolean isIngress() {
    return role.equals(INGRESS);
  }

  @Override
  public String toString() {
    return "ActorRef [role=" + role + (key != null ? ", key=" + key : "") + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((role == null) ? 0 : role.hashCode());
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
    ActorRef other = (ActorRef) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    if (role == null) {
      if (other.role != null)
        return false;
    } else if (!role.equals(other.role))
      return false;
    return true;
  }
  
  public static ActorRef of(String role) {
    return new ActorRef(role, null);
  }
  
  public static ActorRef of(String role, String key) {
    return new ActorRef(role, key);
  }
}
