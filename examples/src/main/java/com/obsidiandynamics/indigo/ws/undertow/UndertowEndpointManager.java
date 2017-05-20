package com.obsidiandynamics.indigo.ws.undertow;

import java.util.*;
import java.util.concurrent.*;

import io.undertow.websockets.*;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.*;

public final class UndertowEndpointManager implements WebSocketConnectionCallback {
  private final UndertowEndpointConfig config;
  
  private final UndertowMessageListener listener;
  
  private final Set<UndertowEndpoint> endpoints = new CopyOnWriteArraySet<>();
  
  public UndertowEndpointManager(UndertowEndpointConfig config, UndertowMessageListener listener) {
    this.config = config;
    this.listener = listener;
  }
  
  @Override
  public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
    final UndertowEndpoint endpoint = createEndpoint(channel);
    channel.getReceiveSetter().set(endpoint);
    channel.resumeReceives();
    listener.onConnect(channel);
  }
  
  UndertowEndpoint createEndpoint(WebSocketChannel channel) {
    final UndertowEndpoint endpoint = new UndertowEndpoint(UndertowEndpointManager.this, channel);
    endpoints.add(endpoint);
    return endpoint;
  }
  
  UndertowMessageListener getMessageListener() {
    return listener;
  }
  
  UndertowEndpointConfig getConfig() {
    return config;
  }
  
  Collection<UndertowEndpoint> getEndpoints() {
    return endpoints;
  }
}
