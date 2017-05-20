package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.xnio.*;

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
  }
  
  UndertowEndpoint createEndpoint(WebSocketChannel channel) {
    final UndertowEndpoint endpoint = new UndertowEndpoint(UndertowEndpointManager.this, channel);
//    try {
//      channel.setOption(Options.TCP_NODELAY, false);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
    endpoints.add(endpoint);
    listener.onConnect(channel);
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
