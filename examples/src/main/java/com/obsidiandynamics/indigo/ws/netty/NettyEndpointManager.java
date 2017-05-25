package com.obsidiandynamics.indigo.ws.netty;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.ws.*;

import io.netty.channel.*;

public final class NettyEndpointManager implements WSEndpointManager<NettyEndpoint> {
  private final NettyEndpointConfig config;
  
  private final EndpointListener<? super NettyEndpoint> listener;
  
  private final Map<ChannelHandlerContext, NettyEndpoint> endpoints = new ConcurrentHashMap<>();
  
  public NettyEndpointManager(NettyEndpointConfig config, EndpointListener<? super NettyEndpoint> listener) {
    this.config = config;
    this.listener = listener;
  }

  NettyEndpoint createEndpoint(ChannelHandlerContext context) {
    final NettyEndpoint endpoint = new NettyEndpoint(this, context);
    endpoints.put(context, endpoint);
    listener.onConnect(endpoint);
    return endpoint;
  }
  
  public NettyEndpoint get(ChannelHandlerContext context) {
    return endpoints.get(context);
  }
  
  public NettyEndpoint remove(ChannelHandlerContext context) {
    return endpoints.remove(context);
  }
  
  EndpointListener<? super NettyEndpoint> getListener() {
    return listener;
  }
  
  NettyEndpointConfig getConfig() {
    return config;
  }

  @Override
  public Collection<NettyEndpoint> getEndpoints() {
    return endpoints.values();
  }
}
