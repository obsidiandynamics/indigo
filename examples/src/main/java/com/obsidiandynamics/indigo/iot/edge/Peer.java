package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;

import com.obsidiandynamics.indigo.ws.*;

public interface Peer extends AutoCloseable {
  InetSocketAddress getAddress();
  
  WSEndpoint getEndpoint();
}
