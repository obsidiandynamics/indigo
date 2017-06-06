package com.obsidiandynamics.indigo.ws.jetty;

import java.util.*;
import java.util.concurrent.*;

import org.eclipse.jetty.websocket.server.*;
import org.eclipse.jetty.websocket.servlet.*;

import com.obsidiandynamics.indigo.ws.*;

public final class JettyEndpointManager extends WebSocketHandler implements WSEndpointManager<JettyEndpoint> {
  private final int idleTimeoutMillis;
  
  private final JettyEndpointConfig config;
  
  private final EndpointListener<? super JettyEndpoint> listener;
  
  private final Set<JettyEndpoint> endpoints = new CopyOnWriteArraySet<>();

  public JettyEndpointManager(int idleTimeoutMillis, JettyEndpointConfig config, EndpointListener<? super JettyEndpoint> listener) {
    this.idleTimeoutMillis = idleTimeoutMillis;
    this.config = config;
    this.listener = listener;
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
    endpoints.add(endpoint);
    return endpoint;
  }
  
  void remove(JettyEndpoint endpoint) {
    endpoints.remove(endpoint);
  }
  
  EndpointListener<? super JettyEndpoint> getListener() {
    return listener;
  }
  
  JettyEndpointConfig getConfig() {
    return config;
  }
  
  @Override
  public Collection<JettyEndpoint> getEndpoints() {
    return endpoints;
  }
}
