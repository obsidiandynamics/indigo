package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;

import com.obsidiandynamics.indigo.ws.*;

final class WSEndpointPeer implements Peer {
  private final WSEndpoint endpoint;

  WSEndpointPeer(WSEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public InetSocketAddress getAddress() {
    return endpoint.getRemoteAddress();
  }

  @Override
  public WSEndpoint getEndpoint() {
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    endpoint.close();
  }
  
  @Override
  public String toString() {
    return String.valueOf(getAddress());
  }
}
