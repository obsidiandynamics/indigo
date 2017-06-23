package com.obsidiandynamics.indigo.ws;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class AbruptCloseTest extends BaseClientServerTest {
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

  private void testClientClose(WSServerFactory<? extends WSEndpoint> serverFactory,
                               WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);
    
    final WSClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);

    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    final WSEndpoint endpoint = openClientEndpoint(serverConfig.port, clientListener);
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onConnect(Mocks.anyNotNull());
      Mockito.verify(clientListener).onConnect(Mocks.anyNotNull());
    });
    
    endpoint.terminate();
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }

  private void testServerClose(WSServerFactory<? extends WSEndpoint> serverFactory,
                               WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);
    
    final WSClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);

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
