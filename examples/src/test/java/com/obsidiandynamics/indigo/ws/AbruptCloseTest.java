package com.obsidiandynamics.indigo.ws;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import java.net.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class AbruptCloseTest implements TestSupport {
  private WSServer<? extends WSEndpoint> server;

  private WSClient<? extends WSEndpoint> client;

  @After
  public void teardown() throws Exception {
    if (server != null) server.close();
    if (client != null) client.close();
  }

  private static WSServerConfig getServerConfig() {
    return new WSServerConfig() {{
      port = SocketTestSupport.getAvailablePort(6667);
      scanIntervalMillis = 10;
    }};
  }

  private static WSClientConfig getClientConfig() {
    return new WSClientConfig();
  }

  @Test
  public void testJtJtClientClose() throws Exception {
    testClientClose(JettyServer.factory(), JettyClient.factory());
  }
  
  @Test
  public void testUtUtClientClose() throws Exception {
    testClientClose(UndertowServer.factory(), UndertowClient.factory());
  }
  
  @Test
  public void testNtUtClientClose() throws Exception {
    testClientClose(NettyServer.factory(), UndertowClient.factory());
  }

  @Test
  public void testJtJtServerClose() throws Exception {
    testServerClose(JettyServer.factory(), JettyClient.factory());
  }
  
  @Test
  public void testUtUtServerClose() throws Exception {
    testServerClose(UndertowServer.factory(), UndertowClient.factory());
  }
  
  @Test
  public void testNtUtServerClose() throws Exception {
    testServerClose(NettyServer.factory(), UndertowClient.factory());
  }
  
  @SuppressWarnings("unchecked")
  private void createServer(WSServerFactory<? extends WSEndpoint> serverFactory,
                            WSServerConfig serverConfig, WSEndpointListener<WSEndpoint> serverListener) throws Exception {
    server = serverFactory.create(serverConfig, Mocks.logger(WSEndpointListener.class, 
                                                             serverListener,
                                                             new LoggingInterceptor<>("s: ")));
  }
  
  private void createClient(WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    client = clientFactory.create(getClientConfig());
  }
  
  @SuppressWarnings("unchecked")
  private WSEndpoint openClientEndpoint(int port, WSEndpointListener<WSEndpoint> clientListener) throws URISyntaxException, Exception {
    return client.connect(new URI("ws://localhost:" + port + "/"),
                          Mocks.logger(WSEndpointListener.class, 
                                       clientListener,
                                       new LoggingInterceptor<>("c: ")));
  }
  
  private boolean hasServerEndpoint() {
    return ! server.getEndpointManager().getEndpoints().isEmpty();
  }
  
  private WSEndpoint getServerEndpoint() {
    return server.getEndpointManager().getEndpoints().iterator().next();
  }
  
  @SuppressWarnings("unchecked")
  private static WSEndpointListener<WSEndpoint> createMockListener() {
    return Mockito.mock(WSEndpointListener.class);
  }

  private void testClientClose(WSServerFactory<? extends WSEndpoint> serverFactory,
                               WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getServerConfig();
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);
    createClient(clientFactory);

    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    final WSEndpoint endpoint = openClientEndpoint(serverConfig.port, clientListener);
    endpoint.terminate();
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }

  private void testServerClose(WSServerFactory<? extends WSEndpoint> serverFactory,
                               WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getServerConfig();
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);
    createClient(clientFactory);

    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    openClientEndpoint(serverConfig.port, clientListener);
    
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).until(this::hasServerEndpoint);
    
    final WSEndpoint endpoint = getServerEndpoint();
    endpoint.terminate();
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }
}
