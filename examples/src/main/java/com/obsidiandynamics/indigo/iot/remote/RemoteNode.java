package com.obsidiandynamics.indigo.iot.remote;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.frame.Wire.*;
import com.obsidiandynamics.indigo.socketx.*;

public final class RemoteNode implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteNode.class);
  
  private final XClient<?> client;
  
  private final Wire wire;
  
  private final List<RemoteNexus> nexuses = new CopyOnWriteArrayList<>();
  
  public RemoteNode(XClientFactory<?> clientFactory, XClientConfig config, Wire wire) throws Exception {
    this.client = clientFactory.create(config);
    this.wire = wire;
  }
  
  public RemoteNexus open(URI uri, RemoteNexusHandler handler) throws Exception {
    if (LOG.isDebugEnabled()) LOG.debug("Connecting to {}", uri);
    final RemoteNexus nexus = new RemoteNexus(RemoteNode.this);
    final EndpointAdapter<XEndpoint> adapter = new EndpointAdapter<>(RemoteNode.this, nexus, handler);
    client.connect(uri, adapter);
    nexuses.add(nexus);
    return nexus;
  }
  
  void addNexus(RemoteNexus nexus) {
    nexuses.add(nexus);
  }
  
  void removeNexus(RemoteNexus nexus) {
    nexuses.remove(nexus);
  }
  
  /**
   *  Obtains the currently connected nexuses.
   *  
   *  @return List of nexuses.
   */
  public List<RemoteNexus> getNexuses() {
    return Collections.unmodifiableList(nexuses);
  }
  
  Wire getWire() {
    return wire;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
  
  public static final class RemoteNodeBuilder {
    private XClientFactory<?> clientFactory;
    private XClientConfig clientConfig = new XClientConfig();
    private Wire wire = new Wire(false, LocationHint.REMOTE);
    
    public RemoteNodeBuilder withClientFactory(XClientFactory<?> clientFactory) {
      this.clientFactory = clientFactory;
      return this;
    }
    
    public RemoteNodeBuilder withClientConfig(XClientConfig clientConfig) {
      this.clientConfig = clientConfig;
      return this;
    }
    
    public RemoteNodeBuilder withWire(Wire wire) {
      this.wire = wire;
      return this;
    }
    
    private void init() throws Exception {
      if (clientFactory == null) {
        clientFactory = (XClientFactory<?>) Class.forName("com.obsidiandynamics.indigo.socketx.undertow.UndertowClient$Factory").newInstance();
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
