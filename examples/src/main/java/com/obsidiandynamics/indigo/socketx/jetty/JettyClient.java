package com.obsidiandynamics.indigo.socketx.jetty;

import java.net.*;
import java.util.*;

import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.websocket.client.*;

import com.obsidiandynamics.indigo.socketx.*;

public final class JettyClient implements XClient<JettyEndpoint> {
  private final HttpClient httpClient;
  
  private final WebSocketClient client;
  
  private final XEndpointScanner<JettyEndpoint> scanner;
  
  private JettyClient(XClientConfig config, HttpClient httpClient) throws Exception {
    this.httpClient = httpClient;
    client = new WebSocketClient(httpClient);
    client.setMaxIdleTimeout(config.idleTimeoutMillis);
    client.start();
    scanner = new XEndpointScanner<>(config.scanIntervalMillis, 0);
  }

  @Override
  public JettyEndpoint connect(URI uri, XEndpointListener<? super JettyEndpoint> listener) throws Exception {
    final JettyEndpoint endpoint = JettyEndpoint.clientOf(scanner, new XEndpointConfig(), listener);
    client.connect(endpoint, uri).get();
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    scanner.close();
    httpClient.stop();
    client.stop();
  }
  
  @Override
  public Collection<JettyEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
  
  public static HttpClient createDefaultHttpClient() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(10_000, 100));
    httpClient.start();
    return httpClient;
  }
  
  public static XClientFactory<JettyEndpoint> factory() {
    return config -> new JettyClient(config, createDefaultHttpClient());
  }
  
  public static XClientFactory<JettyEndpoint> factory(HttpClient httpClient) {
    return config -> new JettyClient(config, httpClient);
  }
}
