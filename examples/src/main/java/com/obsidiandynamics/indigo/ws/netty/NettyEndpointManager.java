package com.obsidiandynamics.indigo.ws.netty;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.ws.*;

import io.netty.channel.*;

public final class NettyEndpointManager implements WSEndpointManager<NettyEndpoint> {
  private final NettyEndpointConfig config;
  
  private final WSListener<NettyEndpoint> listener;
  
  private final Map<ChannelHandlerContext, NettyEndpoint> endpoints = new ConcurrentHashMap<>();
  
  public NettyEndpointManager(NettyEndpointConfig config, WSListener<NettyEndpoint> listener) {
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
  
  WSListener<NettyEndpoint> getListener() {
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
