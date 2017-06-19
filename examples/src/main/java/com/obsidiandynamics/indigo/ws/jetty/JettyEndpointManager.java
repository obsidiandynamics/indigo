package com.obsidiandynamics.indigo.ws.jetty;

import java.util.*;

import org.eclipse.jetty.websocket.server.*;
import org.eclipse.jetty.websocket.servlet.*;

import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.Scanner;

final class JettyEndpointManager extends WebSocketHandler implements WSEndpointManager<JettyEndpoint> {
  private final int idleTimeoutMillis;
  
  private final WSEndpointConfig config;
  
  private final WSEndpointListener<? super JettyEndpoint> listener;
  
  private final Scanner<JettyEndpoint> scanner;

  JettyEndpointManager(Scanner<JettyEndpoint> scanner, int idleTimeoutMillis, 
                       WSEndpointConfig config, WSEndpointListener<? super JettyEndpoint> listener) {
    this.idleTimeoutMillis = idleTimeoutMillis;
    this.config = config;
    this.listener = listener;
    this.scanner = scanner;
  }
  
  @Override
  public void configure(WebSocketServletFactory factory) {
    if (idleTimeoutMillis != 0) {
      factory.getPolicy().setIdleTimeout(idleTimeoutMillis);
    }
    
    factory.setCreator(new WebSocketCreator() {
      @Override public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return createEndpoint();
      }
    });
  }
  
  JettyEndpoint createEndpoint() {
    final JettyEndpoint endpoint = new JettyEndpoint(JettyEndpointManager.this);
    return endpoint;
  }
  
  void add(JettyEndpoint endpoint) {
    scanner.addEndpoint(endpoint);
  }
  
  void remove(JettyEndpoint endpoint) {
    scanner.removeEndpoint(endpoint);
  }
  
  WSEndpointListener<? super JettyEndpoint> getListener() {
    return listener;
  }
  
  WSEndpointConfig getConfig() {
    return config;
  }
  
  @Override
  public Collection<JettyEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
}
