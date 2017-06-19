package com.obsidiandynamics.indigo.ws.jetty;

import java.net.*;

import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.websocket.client.*;

import com.obsidiandynamics.indigo.ws.*;

public final class JettyClient implements WSClient<JettyEndpoint> {
  private final HttpClient httpClient;
  
  private final WebSocketClient client;
  
  private final Scanner<JettyEndpoint> scanner;
  
  private JettyClient(WSClientConfig config, HttpClient httpClient) throws Exception {
    this.httpClient = httpClient;
    client = new WebSocketClient(httpClient);
    client.setMaxIdleTimeout(config.idleTimeoutMillis);
    client.start();
    scanner = new Scanner<>(config.scanIntervalMillis, 0);
  }

  @Override
  public JettyEndpoint connect(URI uri, WSEndpointListener<? super JettyEndpoint> listener) throws Exception {
    final JettyEndpoint endpoint = JettyEndpoint.clientOf(scanner, new WSEndpointConfig(), listener);
    client.connect(endpoint, uri).get();
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    scanner.close();
    httpClient.stop();
    client.stop();
  }
  
  public static HttpClient createDefaultHttpClient() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(10_000, 100));
    httpClient.start();
    return httpClient;
  }
  
  public static WSClientFactory<JettyEndpoint> factory() {
    return config -> new JettyClient(config, createDefaultHttpClient());
  }
  
  public static WSClientFactory<JettyEndpoint> factory(HttpClient httpClient) {
    return config -> new JettyClient(config, httpClient);
  }
}
