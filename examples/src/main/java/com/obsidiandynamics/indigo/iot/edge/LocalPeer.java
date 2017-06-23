package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;

import com.obsidiandynamics.indigo.socketx.*;

final class LocalPeer implements Peer {
  private static final LocalPeer INSTANCE = new LocalPeer();
  
  static LocalPeer instance() { return INSTANCE; }
  
  @Override
  public InetSocketAddress getAddress() {
    return null;
  }

  @Override
  public XEndpoint getEndpoint() {
    return null;
  }
  
  @Override
  public void close() {}

  @Override
  public String toString() {
    return "local";
  }
}
