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
  private static final boolean NO_DELAY = false;
  
  private final UndertowEndpointConfig config;
  
  private final WSListener<UndertowEndpoint> listener;
  
  private final Set<UndertowEndpoint> endpoints = new CopyOnWriteArraySet<>();
  
  public UndertowEndpointManager(UndertowEndpointConfig config, WSListener<UndertowEndpoint> listener) {
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
      channel.setOption(Options.TCP_NODELAY, NO_DELAY);
    } catch (IOException e) {
      e.printStackTrace();
    }
    endpoints.add(endpoint);
    listener.onConnect(endpoint);
    return endpoint;
  }
  
  WSListener<UndertowEndpoint> getListener() {
    return listener;
  }
  
  UndertowEndpointConfig getConfig() {
    return config;
  }
  
  @Override
  public Collection<UndertowEndpoint> getEndpoints() {
    return endpoints;
  }
}
