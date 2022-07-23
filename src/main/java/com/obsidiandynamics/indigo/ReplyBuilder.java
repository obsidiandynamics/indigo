package com.obsidiandynamics.indigo;


public final class ReplyBuilder {
  private final Activation activation;
  
  private final Message message;
  
  ReplyBuilder(Activation activation, Message message) {
    this.activation = activation;
    this.message = message; 
  }
  
  public void tell() {
    tell(null);
  }
  
  public void tell(Object responseBody) {
    final boolean isResponse = message.requestId() != null;
    // Solicited responses go through, but unsolicited ones are silently dropped. This relieves
    // services from having to program defensively, only sending replies when the consumer has
    // requested them with an <code>ask()</code>. It also prevents a consumer using <code>tell()</code>
    // from receiving an unsolicited message.
    if (isResponse) {
      activation.send(new Message(activation.ref, message.from(), responseBody, message.requestId(), true));
    }
  }
}