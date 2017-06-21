package com.obsidiandynamics.indigo.ws;

import java.net.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;

public abstract class BaseClientServerTest implements TestSupport {
  protected WSServer<? extends WSEndpoint> server;

  protected WSClient<? extends WSEndpoint> client;

  @After
  public void teardown() throws Exception {
    cleanup();
  }
  
  protected final void cleanup() throws Exception {
    if (server != null) server.close();
    if (client != null) client.close();
    
    server = null;
    client = null;
  }

  protected static WSServerConfig getDefaultServerConfig() {
    return new WSServerConfig() {{
      port = SocketTestSupport.getAvailablePort(6667);
    }};
  }

  protected static WSClientConfig getDefaultClientConfig() {
    return new WSClientConfig();
  }
  
  @SuppressWarnings("unchecked")
  protected final void createServer(WSServerFactory<? extends WSEndpoint> serverFactory,
                                    WSServerConfig config, WSEndpointListener<WSEndpoint> serverListener) throws Exception {
    server = serverFactory.create(config, Mocks.logger(WSEndpointListener.class, 
                                                             serverListener,
                                                             new LoggingInterceptor<>("s: ")));
  }
  
  protected final void createClient(WSClientFactory<? extends WSEndpoint> clientFactory, WSClientConfig config) throws Exception {
    client = clientFactory.create(config);
  }
  
  @SuppressWarnings("unchecked")
  protected final WSEndpoint openClientEndpoint(int port, WSEndpointListener<WSEndpoint> clientListener) throws URISyntaxException, Exception {
    return client.connect(new URI("ws://localhost:" + port + "/"),
                          Mocks.logger(WSEndpointListener.class, 
                                       clientListener,
                                       new LoggingInterceptor<>("c: ")));
  }
  
  protected final boolean hasServerEndpoint() {
    return ! server.getEndpointManager().getEndpoints().isEmpty();
  }
  
  protected final WSEndpoint getServerEndpoint() {
    return server.getEndpointManager().getEndpoints().iterator().next();
  }
  
  @SuppressWarnings("unchecked")
  protected static WSEndpointListener<WSEndpoint> createMockListener() {
    return Mockito.mock(WSEndpointListener.class);
  }
}
