package com.obsidiandynamics.indigo.ws.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.thread.*;

import com.obsidiandynamics.indigo.ws.*;

public final class JettyServer implements WSServer<JettyEndpoint> {
  private final JettyEndpointManager manager;
  private final Server server;
  
  public JettyServer(int port, String contextPath, JettyEndpointManager manager) throws Exception {
    this.manager = manager;
    server = new Server(new QueuedThreadPool(100));
    final ServerConnector connector = new ServerConnector(server);
    connector.setPort(port);
    server.setConnectors(new Connector[]{connector});
    
    final ContextHandler context = new ContextHandler(contextPath);
    server.setHandler(context);
    context.setHandler(manager);
    server.start();
  }
  
  @Override
  public void close() throws Exception {
    server.stop();
  }

  @Override
  public WSEndpointManager<JettyEndpoint> getEndpointManager() {
    return manager;
  }
  
  public static WSServerFactory<JettyEndpoint> factory() {
    return (config, listener) -> {
      final JettyEndpointManager manager = new JettyEndpointManager(config.idleTimeoutMillis, new JettyEndpointConfig() {{
        highWaterMark = config.highWaterMark;
      }}, listener);
      return new JettyServer(config.port, config.contextPath, manager);
    };
  }
}