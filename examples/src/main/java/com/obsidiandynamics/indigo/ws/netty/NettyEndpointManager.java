package com.obsidiandynamics.indigo.ws.netty;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.Scanner;

import io.netty.channel.*;

public final class NettyEndpointManager implements WSEndpointManager<NettyEndpoint> {
  private final NettyEndpointConfig config;
  
  private final WSEndpointListener<? super NettyEndpoint> listener;
  
  private final Map<ChannelHandlerContext, NettyEndpoint> endpoints = new ConcurrentHashMap<>();
  
  private final Scanner<NettyEndpoint> scanner;
  
  public NettyEndpointManager(Scanner<NettyEndpoint> scanner, NettyEndpointConfig config, 
                              WSEndpointListener<? super NettyEndpoint> listener) {
    this.scanner = scanner;
    this.config = config;
    this.listener = listener;
  }

  NettyEndpoint createEndpoint(ChannelHandlerContext context) {
    final NettyEndpoint endpoint = new NettyEndpoint(this, context);
    endpoints.put(context, endpoint);
    scanner.addEndpoint(endpoint);
    listener.onConnect(endpoint);
    return endpoint;
  }
  
  public NettyEndpoint get(ChannelHandlerContext context) {
    return endpoints.get(context);
  }
  
  NettyEndpoint remove(ChannelHandlerContext context) {
    final NettyEndpoint endpoint = endpoints.remove(context);
    scanner.removeEndpoint(endpoint);
    return endpoint;
  }
  
  WSEndpointListener<? super NettyEndpoint> getListener() {
    return listener;
  }
  
  NettyEndpointConfig getConfig() {
    return config;
  }

  @Override
  public Collection<NettyEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
}
