package com.obsidiandynamics.indigo;

public final class FaultException extends Exception {
  private static final long serialVersionUID = 1L;
  
  private final Object reason;
  
  FaultException(Object reason) {
    super(String.valueOf(reason));
    this.reason = reason;
  }
  
  public Object getReason() {
    return reason;
  }
}
