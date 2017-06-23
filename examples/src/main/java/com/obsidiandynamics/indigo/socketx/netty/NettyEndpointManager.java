package com.obsidiandynamics.indigo.socketx.netty;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.socketx.*;

import io.netty.channel.*;

final class NettyEndpointManager implements XEndpointManager<NettyEndpoint> {
  private final XEndpointConfig config;
  
  private final XEndpointListener<? super NettyEndpoint> listener;
  
  private final Map<Channel, NettyEndpoint> endpoints = new ConcurrentHashMap<>();
  
  private final XEndpointScanner<NettyEndpoint> scanner;
  
  NettyEndpointManager(XEndpointScanner<NettyEndpoint> scanner, XEndpointConfig config, 
                       XEndpointListener<? super NettyEndpoint> listener) {
    this.scanner = scanner;
    this.config = config;
    this.listener = listener;
  }

  NettyEndpoint createEndpoint(ChannelHandlerContext context) {
    final NettyEndpoint endpoint = new NettyEndpoint(this, context);
    endpoints.put(context.channel(), endpoint);
    scanner.addEndpoint(endpoint);
    listener.onConnect(endpoint);
    return endpoint;
  }
  
  NettyEndpoint get(Channel channel) {
    return endpoints.get(channel);
  }
  
  NettyEndpoint remove(Channel channel) {
    final NettyEndpoint endpoint = endpoints.remove(channel);
    scanner.removeEndpoint(endpoint);
    return endpoint;
  }
  
  XEndpointListener<? super NettyEndpoint> getListener() {
    return listener;
  }
  
  XEndpointConfig getConfig() {
    return config;
  }

  @Override
  public Collection<NettyEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
}
