package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;

import com.obsidiandynamics.indigo.ws.*;

final class LocalPeer implements Peer {
  private static final LocalPeer INSTANCE = new LocalPeer();
  
  static LocalPeer instance() { return INSTANCE; }
  
  @Override
  public InetSocketAddress getAddress() {
    return null;
  }

  @Override
  public WSEndpoint getEndpoint() {
    return null;
  }
  
  @Override
  public void close() {}

  @Override
  public String toString() {
    return "local";
  }
}
