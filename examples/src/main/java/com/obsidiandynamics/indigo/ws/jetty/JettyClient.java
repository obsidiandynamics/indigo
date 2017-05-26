package com.obsidiandynamics.indigo.ws.jetty;

import java.net.*;

import org.eclipse.jetty.client.*;
import org.eclipse.jetty.websocket.client.*;

import com.obsidiandynamics.indigo.ws.*;

public class JettyClient implements WSClient<JettyEndpoint> {
  private final HttpClient httpClient;
  
  private final WebSocketClient client;
  
  public JettyClient(WSClientConfig config, HttpClient httpClient) throws Exception {
    this.httpClient = httpClient;
    client = new WebSocketClient(httpClient);
    client.setMaxIdleTimeout(config.idleTimeoutMillis);
    client.start();
  }

  @Override
  public JettyEndpoint connect(URI uri, EndpointListener<? super JettyEndpoint> listener) throws Exception {
    final JettyEndpoint endpoint = JettyEndpoint.clientOf(new JettyEndpointConfig(), listener);
    client.connect(endpoint, uri).get();
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    httpClient.stop();
    client.stop();
  }
  
  public static WSClientFactory<JettyEndpoint> factory(HttpClient httpClient) {
    return config -> new JettyClient(config, httpClient);
  }
}
