package com.obsidiandynamics.indigo.socketx;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.socketx.jetty.*;
import com.obsidiandynamics.indigo.socketx.netty.*;
import com.obsidiandynamics.indigo.socketx.undertow.*;
import com.obsidiandynamics.indigo.util.*;

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
  
  private void testClientTimeout(XServerFactory<? extends XEndpoint> serverFactory,
                                 XClientFactory<? extends XEndpoint> clientFactory,
                                 int idleTimeoutMillis) throws Exception {
    final XServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    final XEndpointListener<XEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final XClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.idleTimeoutMillis = idleTimeoutMillis;
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);

    final XEndpointListener<XEndpoint> clientListener = createMockListener();
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

  private void testServerTimeout(XServerFactory<? extends XEndpoint> serverFactory,
                                 XClientFactory<? extends XEndpoint> clientFactory,
                                 int idleTimeoutMillis) throws Exception {
    final XServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    serverConfig.idleTimeoutMillis = idleTimeoutMillis;
    final XEndpointListener<XEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final XClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);

    final XEndpointListener<XEndpoint> clientListener = createMockListener();
    openClientEndpoint(serverConfig.port, clientListener);
    await().dontCatchUncaughtExceptions().atMost(10, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }
}
