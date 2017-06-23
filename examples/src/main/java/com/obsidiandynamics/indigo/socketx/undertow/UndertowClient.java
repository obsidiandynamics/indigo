package com.obsidiandynamics.indigo.socketx.undertow;

import java.io.*;
import java.net.*;
import java.util.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.socketx.*;

import io.undertow.connector.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.core.*;

public final class UndertowClient implements XClient<UndertowEndpoint> {
  private final XClientConfig config;
  
  private final XnioWorker worker;
  
  private final int bufferSize;
  
  private final XEndpointScanner<UndertowEndpoint> scanner;
  
  private UndertowClient(XClientConfig config, XnioWorker worker, int bufferSize) {
    this.config = config;
    this.worker = worker;
    this.bufferSize = bufferSize;
    scanner = new XEndpointScanner<>(config.scanIntervalMillis, 0);
  }

  @Override
  public UndertowEndpoint connect(URI uri, XEndpointListener<? super UndertowEndpoint> listener) throws Exception {
    final ByteBufferPool pool = new DefaultByteBufferPool(false, bufferSize);
    final WebSocketChannel channel = WebSocketClient.connectionBuilder(worker, pool, uri).connect().get();
    if (config.hasIdleTimeout()) {
      channel.setIdleTimeout(config.idleTimeoutMillis);
    }
    final UndertowEndpoint endpoint = UndertowEndpoint.clientOf(scanner, channel, new XEndpointConfig(), listener);
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
  
  public static final class Factory implements XClientFactory<UndertowEndpoint> {
    @Override public XClient<UndertowEndpoint> create(XClientConfig config) throws Exception {
      return new UndertowClient(config, createDefaultXnioWorker(), 1024);
    }
  }
  
  public static XClientFactory<UndertowEndpoint> factory() {
    return new Factory();
  }
  
  public static XClientFactory<UndertowEndpoint> factory(XnioWorker worker, int bufferSize) {
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
