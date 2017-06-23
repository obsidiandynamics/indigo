package com.obsidiandynamics.indigo.socketx.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.thread.*;

import com.obsidiandynamics.indigo.socketx.*;

public final class JettyServer implements XServer<JettyEndpoint> {
  private final JettyEndpointManager manager;
  private final Server server;
  private final XEndpointScanner<JettyEndpoint> scanner;
  
  private JettyServer(XServerConfig config,
                      XEndpointListener<? super JettyEndpoint> listener) throws Exception {
    server = new Server(new QueuedThreadPool(100));
    final ServerConnector connector = new ServerConnector(server);
    connector.setPort(config.port);
    server.setConnectors(new Connector[]{connector});
    
    final ContextHandler context = new ContextHandler(config.contextPath);
    server.setHandler(context);

    scanner = new XEndpointScanner<>(config.scanIntervalMillis, config.pingIntervalMillis);
    manager = new JettyEndpointManager(scanner, config.idleTimeoutMillis, 
                                       config.endpointConfig, listener);
    context.setHandler(manager);
    server.start();
  }
  
  @Override
  public void close() throws Exception {
    scanner.close();
    server.stop();
  }

  @Override
  public XEndpointManager<JettyEndpoint> getEndpointManager() {
    return manager;
  }
  
  public static XServerFactory<JettyEndpoint> factory() {
    return JettyServer::new;
  }
}