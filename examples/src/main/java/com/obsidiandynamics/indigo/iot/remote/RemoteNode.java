package com.obsidiandynamics.indigo.iot.remote;

import java.net.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RemoteNode implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteNode.class);
  
  private final WSClient<?> client;
  
  private final Wire wire;
  
  public RemoteNode(WSClientFactory<?> clientFactory, WSClientConfig config, Wire wire) throws Exception {
    this.client = clientFactory.create(config);
    this.wire = wire;
  }
  
  public RemoteNexus open(URI uri, RemoteNexusHandler handler) throws Exception {
    if (LOG.isDebugEnabled()) LOG.debug("Connecting to {}", uri);
    final RemoteNexus nexus = new RemoteNexus(RemoteNode.this);
    final EndpointAdapter<WSEndpoint> adapter = new EndpointAdapter<>(RemoteNode.this, nexus, handler);
    client.connect(uri, adapter);
    return nexus;
  }
  
  Wire getWire() {
    return wire;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
