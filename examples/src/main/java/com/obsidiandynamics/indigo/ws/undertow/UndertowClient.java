package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.net.*;
import java.util.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.Scanner;

import io.undertow.connector.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.core.*;

public final class UndertowClient implements WSClient<UndertowEndpoint> {
  private final WSClientConfig config;
  
  private final XnioWorker worker;
  
  private final int bufferSize;
  
  private final Scanner<UndertowEndpoint> scanner;
  
  private UndertowClient(WSClientConfig config, XnioWorker worker, int bufferSize) {
    this.config = config;
    this.worker = worker;
    this.bufferSize = bufferSize;
    scanner = new Scanner<>(config.scanIntervalMillis, 0);
  }

  @Override
  public UndertowEndpoint connect(URI uri, WSEndpointListener<? super UndertowEndpoint> listener) throws Exception {
    final ByteBufferPool pool = new DefaultByteBufferPool(false, bufferSize);
    final WebSocketChannel channel = WebSocketClient.connectionBuilder(worker, pool, uri).connect().get();
    if (config.hasIdleTimeout()) {
      channel.setIdleTimeout(config.idleTimeoutMillis);
    }
    final UndertowEndpoint endpoint = UndertowEndpoint.clientOf(scanner, channel, new WSEndpointConfig(), listener);
    channel.getReceiveSetter().set(endpoint);
    channel.resumeReceives();
    return endpoint;
  }

  @Override
  public void close() throws Exception {
    scanner.close();
    worker.shutdown();
    worker.awaitTermination();
  }
  
  @Override
  public Collection<UndertowEndpoint> getEndpoints() {
    return scanner.getEndpoints();
  }
  
  public static final class Factory implements WSClientFactory<UndertowEndpoint> {
    @Override public WSClient<UndertowEndpoint> create(WSClientConfig config) throws Exception {
      return new UndertowClient(config, createDefaultXnioWorker(), 1024);
    }
  }
  
  public static WSClientFactory<UndertowEndpoint> factory() {
    return new Factory();
  }
  
  public static WSClientFactory<UndertowEndpoint> factory(XnioWorker worker, int bufferSize) {
    return config -> new UndertowClient(config, worker, bufferSize);
  }
  
  public static XnioWorker createDefaultXnioWorker() throws IllegalArgumentException, IOException {
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
