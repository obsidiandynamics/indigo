package com.obsidiandynamics.indigo.iot.client;

import java.net.*;

import com.obsidiandynamics.indigo.ws.*;

public final class Session {
  private final WSEndpoint<?> endpoint;
  
  Session(WSEndpoint<?> endpoint) {
    this.endpoint = endpoint;
  }
  
  public InetSocketAddress getRemoteAddress() {
    return endpoint.getRemoteAddress();
  }

  @Override
  public String toString() {
    return "Session [remote=" + getRemoteAddress() + "]";
  }
}
