package com.obsidiandynamics.indigo.socketx.undertow;

import java.io.*;
import java.util.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.socketx.*;

import io.undertow.websockets.*;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.*;

final class UndertowEndpointManager implements WebSocketConnectionCallback, XEndpointManager<UndertowEndpoint> {
  private static final boolean NODELAY = true;
  
  private final int idleTimeoutMillis;
  
  private final XEndpointConfig config;
  
  private final XEndpointListener<? super UndertowEndpoint> listener;
  
  private final XEndpointScanner<UndertowEndpoint> scanner;
  
  UndertowEndpointManager(XEndpointScanner<UndertowEndpoint> scanner, int idleTimeoutMillis, XEndpointConfig config, 
                          XEndpointListener<? super UndertowEndpoint> listener) {
    this.idleTimeoutMillis = idleTimeoutMillis;
    this.config = config;
    this.listener = listener;
    this.scanner = scanner;
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
    if (idleTimeoutMillis != 0) {
      channel.setIdleTimeout(idleTimeoutMillis);
    }
    scanner.addEndpoint(endpoint);
    listener.onConnect(endpoint);
    return endpoint;
  }
  
  XEndpointListener<? super UndertowEndpoint> getListener() {
    return listener;
  }
  
  XEndpointConfig getConfig() {
    return config;
  }
  
  @Override
  public Collection<UndertowEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
  
  void remove(UndertowEndpoint endpoint) {
    scanner.removeEndpoint(endpoint);
  }
}
