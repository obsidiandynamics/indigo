package com.obsidiandynamics.indigo.ws;

import java.util.*;
import java.util.concurrent.*;

import org.eclipse.jetty.websocket.server.*;
import org.eclipse.jetty.websocket.servlet.*;

public final class EndpointManager extends WebSocketHandler {
  private final int idleTimeoutMillis;
  
  private final EndpointConfig config;
  
  private final MessageListener listener;
  
  private final Set<Endpoint> endpoints = new CopyOnWriteArraySet<>();

  public EndpointManager(int idleTimeoutMillis, EndpointConfig config, MessageListener listener) {
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
  
  Endpoint createEndpoint() {
    final Endpoint endpoint = new Endpoint(EndpointManager.this);
    endpoints.add(endpoint);
    return endpoint;
  }
  
  MessageListener getMessageListener() {
    return listener;
  }
  
  EndpointConfig getConfig() {
    return config;
  }
  
  Collection<Endpoint> getEndpoints() {
    return endpoints;
  }
}
