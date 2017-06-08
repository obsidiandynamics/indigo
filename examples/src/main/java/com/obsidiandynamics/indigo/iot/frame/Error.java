package com.obsidiandynamics.indigo.iot.frame;

public abstract class Error {
  private final String description;
  
  protected Error(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }  
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((description == null) ? 0 : description.hashCode());
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
    Error other = (Error) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    return true;
  }
  
  @Override
  public abstract String toString();
}
