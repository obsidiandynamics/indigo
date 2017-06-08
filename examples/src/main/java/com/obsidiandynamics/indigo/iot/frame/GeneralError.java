package com.obsidiandynamics.indigo.iot.frame;

public final class GeneralError extends Error {
  public static String JSON_TYPE_NAME = "General";
  
  public GeneralError(String description) {
    super(description);
  }
  
  @Override
  public int hashCode() {
    int result = super.hashCode();
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
    return true;
  }

  @Override
  public String toString() {
    return "GeneralError [description=" + getDescription() + "]";
  }
}
