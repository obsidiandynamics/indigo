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
  
  public static final class RemoteNodeBuilder {
    private WSClientFactory<?> clientFactory;
    private WSClientConfig clientConfig = new WSClientConfig();
    private Wire wire = new Wire(false);
    
    public RemoteNodeBuilder withClientFactory(WSClientFactory<?> clientFactory) {
      this.clientFactory = clientFactory;
      return this;
    }
    
    public RemoteNodeBuilder withClientConfig(WSClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }
    
    public RemoteNodeBuilder withWire(Wire wire) {
      this.wire = wire;
      return this;
    }
    
    private void init() throws Exception {
      if (clientFactory == null) {
        clientFactory = (WSClientFactory<?>) Class.forName("com.obsidiandynamics.indigo.ws.undertow.UndertowClient$Factory").newInstance();
      }
    }
    
    public RemoteNode build() throws Exception {
      init();
      return new RemoteNode(clientFactory, clientConfig, wire);
    }
  }
  
  public static RemoteNodeBuilder builder() {
    return new RemoteNodeBuilder();
  }
}
