package com.obsidiandynamics.indigo;

public final class Fault implements Signal {
  private final FaultType type;
  
  private final Message originalMessage;
  
  private final Object reason;

  Fault(FaultType type, Message originalMessage, Object reason) {
    this.type = type;
    this.originalMessage = originalMessage;
    this.reason = reason;
  }

  public FaultType getType() {
    return type;
  }

  public Message getOriginalMessage() {
    return originalMessage;
  }

  public Object getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return "Fault [type=" + type + ", originalMessage=" + originalMessage + ", reason=" + reason + "]";
  }
}
