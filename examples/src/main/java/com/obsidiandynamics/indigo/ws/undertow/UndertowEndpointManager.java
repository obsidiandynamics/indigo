package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.util.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.Scanner;

import io.undertow.websockets.*;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.*;

public final class UndertowEndpointManager implements WebSocketConnectionCallback, WSEndpointManager<UndertowEndpoint> {
  private static final boolean NODELAY = true;
  
  private final int idleTimeoutMillis;
  
  private final UndertowEndpointConfig config;
  
  private final WSEndpointListener<? super UndertowEndpoint> listener;
  
  private final Scanner<UndertowEndpoint> scanner;
  
  public UndertowEndpointManager(Scanner<UndertowEndpoint> scanner, int idleTimeoutMillis, UndertowEndpointConfig config, 
                                 WSEndpointListener<? super UndertowEndpoint> listener) {
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
  
  WSEndpointListener<? super UndertowEndpoint> getListener() {
    return listener;
  }
  
  UndertowEndpointConfig getConfig() {
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
