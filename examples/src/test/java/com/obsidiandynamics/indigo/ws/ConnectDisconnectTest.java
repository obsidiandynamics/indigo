package com.obsidiandynamics.indigo.ws;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;

import java.util.*;

import org.junit.Test;
import org.mockito.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

import junit.framework.*;

public final class ConnectDisconnectTest extends BaseClientServerTest {
  private static final int CYCLES = 2;
  private static final int CONNECTIONS = 10;
  private static final int PROGRESS_INTERVAL = 10;
  private static final int MAX_PORT_USE_COUNT = 10_000;

  @Test
  public void testJtJt() throws Exception {
    test(true, CYCLES, CONNECTIONS, JettyServer.factory(), JettyClient.factory());
    test(false, CYCLES, CONNECTIONS, JettyServer.factory(), JettyClient.factory());
  }

  @Test
  public void testUtUt() throws Exception {
    test(true, CYCLES, CONNECTIONS, UndertowServer.factory(), UndertowClient.factory());
    test(false, CYCLES, CONNECTIONS, UndertowServer.factory(), UndertowClient.factory());
  }

  @Test
  public void testNtUt() throws Exception {
    test(true, CYCLES, CONNECTIONS, NettyServer.factory(), UndertowClient.factory());
    test(false, CYCLES, CONNECTIONS, NettyServer.factory(), UndertowClient.factory());
  }

  private void test(boolean clean, int cycles, int connections,
                    WSServerFactory<? extends WSEndpoint> serverFactory,
                    WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    for (int cycle = 0; cycle < cycles; cycle++) {
      test(clean, connections, serverFactory, clientFactory);
      cleanup();
      if (PROGRESS_INTERVAL != 0 && cycle % PROGRESS_INTERVAL == PROGRESS_INTERVAL - 1) {
        LOG_STREAM.format("cycle %,d\n", cycle);
      }
    }
  }

  private void test(boolean clean, int connections,
                    WSServerFactory<? extends WSEndpoint> serverFactory,
                    WSClientFactory<? extends WSEndpoint> clientFactory) throws Exception {
    final WSServerConfig serverConfig = getDefaultServerConfig();
    serverConfig.scanIntervalMillis = 1;
    final WSEndpointListener<WSEndpoint> serverListener = createMockListener();
    createServer(serverFactory, serverConfig, serverListener);

    final WSClientConfig clientConfig = getDefaultClientConfig();
    clientConfig.scanIntervalMillis = 1;
    createClient(clientFactory, clientConfig);
    final WSEndpointListener<WSEndpoint> clientListener = createMockListener();
    final List<WSEndpoint> endpoints = new ArrayList<>(connections);
    
    // connect all endpoints
    for (int i = 0; i < connections; i++) {
      endpoints.add(openClientEndpoint(serverConfig.port, clientListener));
    }

    // assert connections on server
    await().dontCatchUncaughtExceptions().atMost(60, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener, Mockito.times(connections)).onConnect(Mocks.anyNotNull());
      Mockito.verify(clientListener, Mockito.times(connections)).onConnect(Mocks.anyNotNull());
    });

    // disconnect all endpoints and await closure
    for (WSEndpoint endpoint : endpoints) {
      if (clean) endpoint.close();
      else endpoint.terminate();
    }
    for (WSEndpoint endpoint : endpoints) {
      endpoint.awaitClose(Integer.MAX_VALUE);
    }
    
    // assert disconnections on server
    await().dontCatchUncaughtExceptions().atMost(60, SECONDS).untilAsserted(() -> {
      Mockito.verify(serverListener, Mockito.times(connections)).onClose(Mocks.anyNotNull());
      Mockito.verify(clientListener, Mockito.times(connections)).onClose(Mocks.anyNotNull());
    });
    
    TestCase.assertEquals(0, server.getEndpointManager().getEndpoints().size());
    TestCase.assertEquals(0, client.getEndpoints().size());
    
    SocketTestSupport.drainPort(serverConfig.port, MAX_PORT_USE_COUNT);
  }
}