package com.obsidiandynamics.indigo.ws.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.websocket.server.*;

public final class JettyServer implements AutoCloseable {
  private final Server server;
  
  public JettyServer(int port, String contextPath, WebSocketHandler wsHandler) throws Exception {
    server = new Server(new QueuedThreadPool(100));
    final ServerConnector connector = new ServerConnector(server);
    connector.setPort(port);
    server.setConnectors(new Connector[]{connector});
    
    final ContextHandler context = new ContextHandler(contextPath);
    server.setHandler(context);
    context.setHandler(wsHandler);
    server.start();
  }
  
  @Override
  public void close() throws Exception {
    server.stop();
  }
}