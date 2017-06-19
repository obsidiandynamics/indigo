package com.obsidiandynamics.indigo.ws;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class KeepAliveTest extends BaseClientServerTest {
  private static final int CYCLES = 2;
  
  @Test
  public void testJtJtKeepAlive() throws Exception {
    testKeepAlive(CYCLES, JettyServer.factory(), JettyClient.factory());
  }

  @Test
  public void testUtUtKeepAlive() throws Exception {
    testKeepAlive(CYCLES, UndertowServer.factory(), UndertowClient.factory());
  }

  @Test
  public void testNtUtKeepAlive() throws Exception {
    testKeepAlive(CYCLES, NettyServer.factory(), UndertowClient.factory());
  }

  private void testKeepAlive(int n,
                             WSServerFactory<? extends WSEndpoint> serverFactory,
                             WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    for (int i = 0; i < n; i++) {
      testKeepAlive(serverFactory, clientFactory);
      cleanup();
    }
  }

  private void testKeepAlive(WSServerFactory<? extends WSEndpoint> serverFactory,
                             WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    serverConfig.pingIntervalMillis = 1;
    serverConfig.idleTimeoutMillis = 2000;
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final WSClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    clientConfig.idleTimeoutMillis = 2000;
    createClient(clientFactory, clientConfig);

    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    openClientEndpoint(serverConfig.port, clientListener);
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(clientListener, Mockito.atLeastOnce()).onPing(Mocks.anyNotNull());
      Mockito.verify(serverListener, Mockito.atLeastOnce()).onPong(Mocks.anyNotNull());
    });
  }
}