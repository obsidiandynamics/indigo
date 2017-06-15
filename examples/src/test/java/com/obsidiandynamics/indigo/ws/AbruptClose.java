package com.obsidiandynamics.indigo.ws;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.undertow.*;
import org.junit.*;

import java.net.*;
import java.nio.*;

public final class AbruptClose implements TestSupport {
  private WSServer<?> server;

  private WSClient<?> client;

  @After
  public void teardown() throws Exception {
    if (server != null) server.close();
    if (client != null) client.close();
  }

  private static WSServerConfig getServerConfig() {
    return new WSServerConfig() {{
      port = SocketTestSupport.getAvailablePort(6667);
    }};
  }

  private static WSClientConfig getClientConfig() {
    return new WSClientConfig();
  }

  @Test
  public void testUtUtClientClose() throws Exception {
    testClientClose(UndertowServer.factory(), UndertowClient.factory());
  }

  @SuppressWarnings("unchecked") private void testClientClose(WSServerFactory<?> serverFactory,
                                                              WSClientFactory<?> clientFactory) throws Exception {
    final EndpointListener<WSEndpoint> serverListener = new EndpointListener<WSEndpoint>() {
      @Override public void onConnect(WSEndpoint endpoint) {
        //        log("%s connected\n", endpoint);
      }

      @Override public void onText(WSEndpoint endpoint, String message) {

      }

      @Override public void onBinary(WSEndpoint endpoint, ByteBuffer message) {

      }

      @Override public void onDisconnect(WSEndpoint endpoint, int statusCode, String reason) {
        //        log("%s disconnected; statusCode: %d, reason: %s\n", endpoint, statusCode, reason);

      }

      @Override public void onClose(WSEndpoint endpoint) {
        //        log("%s closed\n", endpoint);

      }

      @Override public void onError(WSEndpoint endpoint, Throwable cause) {
        //        log("%s error; cause: %s\n", endpoint, cause);
      }
    };
    final WSServerConfig serverConfig = getServerConfig();
    server = serverFactory.create(serverConfig, Mocks.logger(EndpointListener.class, serverListener));
    client = clientFactory.create(getClientConfig());

    final EndpointListener<WSEndpoint> clientListener = new EndpointListener<WSEndpoint>() {
      @Override public void onConnect(WSEndpoint endpoint) {
        //        log("%s connected\n", endpoint);
      }

      @Override public void onText(WSEndpoint endpoint, String message) {

      }

      @Override public void onBinary(WSEndpoint endpoint, ByteBuffer message) {

      }

      @Override public void onDisconnect(WSEndpoint endpoint, int statusCode, String reason) {
        //        log("%s disconnected; statusCode: %d, reason: %s\n", endpoint, statusCode, reason);

      }

      @Override public void onClose(WSEndpoint endpoint) {
        //        log("%s closed\n", endpoint);

      }

      @Override public void onError(WSEndpoint endpoint, Throwable cause) {
        //        log("%s error; cause: %s\n", endpoint, cause);
      }
    };
    final WSEndpoint endpoint = client.connect(new URI("ws://localhost:" + serverConfig.port + "/"),
                                               Mocks.logger(EndpointListener.class, clientListener));
  }
}
