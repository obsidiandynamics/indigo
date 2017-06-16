package com.obsidiandynamics.indigo.ws.netty;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.Scanner;

import io.netty.channel.*;

final class NettyEndpointManager implements WSEndpointManager<NettyEndpoint> {
  private final NettyEndpointConfig config;
  
  private final WSEndpointListener<? super NettyEndpoint> listener;
  
  private final Map<Channel, NettyEndpoint> endpoints = new ConcurrentHashMap<>();
  
  private final Scanner<NettyEndpoint> scanner;
  
  NettyEndpointManager(Scanner<NettyEndpoint> scanner, NettyEndpointConfig config, 
                       WSEndpointListener<? super NettyEndpoint> listener) {
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
