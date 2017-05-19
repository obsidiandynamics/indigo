package com.obsidiandynamics.indigo.adder.ws;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.websocket.server.*;

public final class WSServer implements AutoCloseable {
  private final Server server;
  
  public WSServer(int port, String contextPath, WebSocketHandler wsHandler) throws Exception {
    server = new Server(port);
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