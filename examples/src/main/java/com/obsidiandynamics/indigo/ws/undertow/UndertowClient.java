package com.obsidiandynamics.indigo.ws.undertow;

import java.net.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.ws.*;

import io.undertow.connector.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.core.*;

public class UndertowClient implements WSClient<UndertowEndpoint> {
  private final WSClientConfig config;
  
  private final XnioWorker worker;
  
  private final int bufferSize;
  
  public UndertowClient(WSClientConfig config, XnioWorker worker, int bufferSize) {
    this.config = config;
    this.worker = worker;
    this.bufferSize = bufferSize;
  }

  @Override
  public UndertowEndpoint connect(URI uri, EndpointListener<? super UndertowEndpoint> listener) throws Exception {
    final ByteBufferPool pool = new DefaultByteBufferPool(false, bufferSize);
    final WebSocketChannel channel = WebSocketClient.connectionBuilder(worker, pool, uri)
        .connect().get();
    if (config.hasIdleTimeout()) {
      channel.setIdleTimeout(config.idleTimeoutMillis);
    }
    final UndertowEndpoint endpoint = UndertowEndpoint.clientOf(channel, new UndertowEndpointConfig(), listener);
    channel.getReceiveSetter().set(endpoint);
    channel.resumeReceives();
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    worker.shutdown();
  }
  
  public static WSClientFactory<UndertowEndpoint> factory(XnioWorker worker, int bufferSize) {
    return config -> new UndertowClient(config, worker, bufferSize);
  }
}
