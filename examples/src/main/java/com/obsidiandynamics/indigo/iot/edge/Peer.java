package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;

import com.obsidiandynamics.indigo.socketx.*;

public interface Peer extends AutoCloseable {
  InetSocketAddress getAddress();
  
  XEndpoint getEndpoint();
  
  default boolean hasEndpoint() {
    return getEndpoint() != null;
  }
}
