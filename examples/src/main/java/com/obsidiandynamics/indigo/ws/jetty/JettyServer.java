package com.obsidiandynamics.indigo.ws.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.thread.*;

import com.obsidiandynamics.indigo.ws.*;

public final class JettyServer implements WSServer<JettyEndpoint> {
  private final JettyEndpointManager manager;
  private final Server server;
  private final Scanner<JettyEndpoint> scanner;
  
  private JettyServer(WSServerConfig config,
                      EndpointListener<? super JettyEndpoint> listener) throws Exception {
    server = new Server(new QueuedThreadPool(100));
    final ServerConnector connector = new ServerConnector(server);
    connector.setPort(config.port);
    server.setConnectors(new Connector[]{connector});
    
    final ContextHandler context = new ContextHandler(config.contextPath);
    server.setHandler(context);

    scanner = new Scanner<>(config.scanIntervalMillis, true);
    final JettyEndpointConfig endpointConfig = new JettyEndpointConfig() {{
      highWaterMark = config.highWaterMark;
    }};
    manager = new JettyEndpointManager(scanner, config.idleTimeoutMillis, 
                                                                  endpointConfig, listener);
    context.setHandler(manager);
    server.start();
  }
  
  @Override
  public void close() throws Exception {
    scanner.close();
    server.stop();
  }

  @Override
  public WSEndpointManager<JettyEndpoint> getEndpointManager() {
    return manager;
  }
  
  public static WSServerFactory<JettyEndpoint> factory() {
    return JettyServer::new;
  }
}