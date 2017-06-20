package com.obsidiandynamics.indigo.ws;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class IdleTimeoutTest extends BaseClientServerTest {
  @Test
  public void testJtJtServerTimeout() throws Exception {
    // Note: Jetty requires more idle time allowance than others, otherwise the connection
    // times out before it is upgraded to a WebSocket.
    testServerTimeout(JettyServer.factory(), JettyClient.factory(), 500);
  }
  
  @Test
  public void testUtUtServerTimeout() throws Exception {
    testServerTimeout(UndertowServer.factory(), UndertowClient.factory(), 1);
  }
  
  @Test
  public void testNtUtServerTimeout() throws Exception {
    testServerTimeout(NettyServer.factory(), UndertowClient.factory(), 1);
  }
  
  @Test
  public void testJtJtClientTimeout() throws Exception {
    // Note: Jetty requires more idle time allowance than others, otherwise the connection
    // times out before it is upgraded to a WebSocket.
    testClientTimeout(JettyServer.factory(), JettyClient.factory(), 500);
  }
  
  @Test
  public void testUtUtClientTimeout() throws Exception {
    testClientTimeout(UndertowServer.factory(), UndertowClient.factory(), 1);
  }
  
  private void testClientTimeout(WSServerFactory<? extends WSEndpoint> serverFactory,
                                 WSClientFactory<? extends WSEndpoint> clientFactory,
                                 int idleTimeoutMillis) throws Exception {
    final WSServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final WSClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.idleTimeoutMillis = idleTimeoutMillis;
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);

    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    openClientEndpoint(serverConfig.port, clientListener);
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onConnect(Mocks.anyNotNull());
      Mockito.verify(clientListener).onConnect(Mocks.anyNotNull());
    });
    
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }

  private void testServerTimeout(WSServerFactory<? extends WSEndpoint> serverFactory,
                                 WSClientFactory<? extends WSEndpoint> clientFactory,
                                 int idleTimeoutMillis) throws Exception {
    final WSServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    serverConfig.idleTimeoutMillis = idleTimeoutMillis;
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final WSClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);

    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    openClientEndpoint(serverConfig.port, clientListener);
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }
}
