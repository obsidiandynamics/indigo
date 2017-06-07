package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.net.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.ws.*;

import io.undertow.connector.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.core.*;

public final class UndertowClient implements WSClient<UndertowEndpoint> {
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
    worker.awaitTermination();
  }
  
  public static final class Factory implements WSClientFactory<UndertowEndpoint> {
    @Override
    public WSClient<UndertowEndpoint> create(WSClientConfig config) throws Exception {
      return new UndertowClient(config, createXnioWorker(), 1024);
    }
  }
  
  public static WSClientFactory<UndertowEndpoint> factory() {
    return new Factory();
  }
  
  public static WSClientFactory<UndertowEndpoint> factory(XnioWorker worker, int bufferSize) {
    return config -> new UndertowClient(config, worker, bufferSize);
  }
  
  private static XnioWorker createXnioWorker() throws IllegalArgumentException, IOException {
    return Xnio.getInstance().createWorker(OptionMap.builder()
                                           .set(Options.WORKER_IO_THREADS, Runtime.getRuntime().availableProcessors())
                                           .set(Options.THREAD_DAEMON, true)
                                           .set(Options.CONNECTION_HIGH_WATER, 1_000_000)
                                           .set(Options.CONNECTION_LOW_WATER, 1_000_000)
                                           .set(Options.WORKER_TASK_CORE_THREADS, 100)
                                           .set(Options.WORKER_TASK_MAX_THREADS, 10_000)
                                           .set(Options.TCP_NODELAY, true)
                                           .getMap());
  }
}
