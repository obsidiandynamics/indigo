package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.ws.*;

import io.undertow.websockets.*;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.*;

public final class UndertowEndpointManager implements WebSocketConnectionCallback, WSEndpointManager<UndertowEndpoint> {
  private static final boolean NODELAY = true;
  
  private final UndertowEndpointConfig config;
  
  private final EndpointListener<? super UndertowEndpoint> listener;
  
  private final Set<UndertowEndpoint> endpoints = new CopyOnWriteArraySet<>();
  
  public UndertowEndpointManager(UndertowEndpointConfig config, EndpointListener<? super UndertowEndpoint> listener) {
    this.config = config;
    this.listener = listener;
  }
  
  @Override
  public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
    final UndertowEndpoint endpoint = createEndpoint(channel);
    channel.getReceiveSetter().set(endpoint);
    channel.resumeReceives();
  }
  
  UndertowEndpoint createEndpoint(WebSocketChannel channel) {
    final UndertowEndpoint endpoint = new UndertowEndpoint(this, channel);
    try {
      channel.setOption(Options.TCP_NODELAY, NODELAY);
    } catch (IOException e) {
      e.printStackTrace();
    }
    endpoints.add(endpoint);
    listener.onConnect(endpoint);
    return endpoint;
  }
  
  EndpointListener<? super UndertowEndpoint> getListener() {
    return listener;
  }
  
  UndertowEndpointConfig getConfig() {
    return config;
  }
  
  @Override
  public Collection<UndertowEndpoint> getEndpoints() {
    return endpoints;
  }
  
  void remove(UndertowEndpoint endpoint) {
    endpoints.remove(endpoint);
  }
}
