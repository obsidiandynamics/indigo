package com.obsidiandynamics.indigo.ws.jetty;

import java.util.*;
import java.util.concurrent.*;

import org.eclipse.jetty.websocket.server.*;
import org.eclipse.jetty.websocket.servlet.*;

public final class JettyEndpointManager extends WebSocketHandler {
  private final int idleTimeoutMillis;
  
  private final JettyEndpointConfig config;
  
  private final JettyMessageListener listener;
  
  private final Set<JettyEndpoint> endpoints = new CopyOnWriteArraySet<>();

  public JettyEndpointManager(int idleTimeoutMillis, JettyEndpointConfig config, JettyMessageListener listener) {
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
  
  JettyMessageListener getMessageListener() {
    return listener;
  }
  
  JettyEndpointConfig getConfig() {
    return config;
  }
  
  Collection<JettyEndpoint> getEndpoints() {
    return endpoints;
  }
}
