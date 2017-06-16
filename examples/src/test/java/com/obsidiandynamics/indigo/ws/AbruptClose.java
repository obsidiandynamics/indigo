package com.obsidiandynamics.indigo.ws;

import java.net.*;
import java.util.concurrent.*;

import org.awaitility.*;
import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class AbruptClose implements TestSupport {
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

  @SuppressWarnings("unchecked") 
  private void testClientClose(WSServerFactory<? extends WSEndpoint> serverFactory,
                               WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getServerConfig();
    final EndpointListener<WSEndpoint> serverListener = Mockito.mock(EndpointListener.class);
    server = serverFactory.create(serverConfig, Mocks.logger(EndpointListener.class, 
                                                             serverListener,
                                                             new LoggingInterceptor<>("s: ")));
    client = clientFactory.create(getClientConfig());

    final EndpointListener<WSEndpoint> clientListener = Mockito.mock(EndpointListener.class);
    final WSEndpoint endpoint = client.connect(new URI("ws://localhost:" + serverConfig.port + "/"),
                                               Mocks.logger(EndpointListener.class, 
                                                            clientListener,
                                                            new LoggingInterceptor<>("c: ")));
    endpoint.terminate();
    Awaitility.await().dontCatchUncaughtExceptions().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener).onClose(Mocks.anyNotNull());
    });
  }
}
