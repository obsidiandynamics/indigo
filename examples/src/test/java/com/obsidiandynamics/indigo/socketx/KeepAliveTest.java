package com.obsidiandynamics.indigo.socketx;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.socketx.jetty.*;
import com.obsidiandynamics.indigo.socketx.netty.*;
import com.obsidiandynamics.indigo.socketx.undertow.*;
import com.obsidiandynamics.indigo.util.*;

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

  private void testKeepAlive(int cycles,
                             XServerFactory<? extends XEndpoint> serverFactory,
                             XClientFactory<? extends XEndpoint> clientFactory) throws Exception {
    for (int cycle = 0; cycle < cycles; cycle++) {
      testKeepAlive(serverFactory, clientFactory);
      cleanup();
    }
  }

  private void testKeepAlive(XServerFactory<? extends XEndpoint> serverFactory,
                             XClientFactory<? extends XEndpoint> clientFactory) throws Exception {
    final XServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    serverConfig.pingIntervalMillis = 1;
    serverConfig.idleTimeoutMillis = 2000;
    final XEndpointListener<XEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final XClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    clientConfig.idleTimeoutMillis = 2000;
    createClient(clientFactory, clientConfig);

    final XEndpointListener<XEndpoint> clientListener = createMockListener();
    openClientEndpoint(serverConfig.port, clientListener);
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onConnect(Mocks.anyNotNull());
      Mockito.verify(clientListener).onConnect(Mocks.anyNotNull());
    });
    
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(clientListener, Mockito.atLeastOnce()).onPing(Mocks.anyNotNull());
      Mockito.verify(serverListener, Mockito.atLeastOnce()).onPong(Mocks.anyNotNull());
    });
  }
}