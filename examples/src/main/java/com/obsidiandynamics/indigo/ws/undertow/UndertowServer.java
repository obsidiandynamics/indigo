package com.obsidiandynamics.indigo.ws.undertow;

import static io.undertow.Handlers.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.ws.*;

import io.undertow.*;

public final class UndertowServer implements WSServer<UndertowEndpoint> {
  private final Undertow server;
  private final UndertowEndpointManager manager;
  private final XnioWorker worker;
  private final Scanner<UndertowEndpoint> scanner;
  
  private UndertowServer(WSServerConfig config,
                         WSEndpointListener<? super UndertowEndpoint> listener) throws Exception {
    final int ioThreads = Runtime.getRuntime().availableProcessors();
    final int coreWorkerThreads = 100;
    final int maxWorkerThreads = coreWorkerThreads * 100;
    
    worker = Xnio.getInstance().createWorker(OptionMap.builder()
                                             .set(Options.WORKER_IO_THREADS, ioThreads)
                                             .set(Options.THREAD_DAEMON, true)
                                             .set(Options.WORKER_TASK_CORE_THREADS, coreWorkerThreads)
                                             .set(Options.WORKER_TASK_MAX_THREADS, maxWorkerThreads)
                                             .set(Options.TCP_NODELAY, true)
                                             .getMap());
    
    scanner = new Scanner<>(config.scanIntervalMillis, true);
    final UndertowEndpointConfig endpointConfig = new UndertowEndpointConfig() {{
      highWaterMark = config.highWaterMark;
    }};
    manager = new UndertowEndpointManager(scanner, config.idleTimeoutMillis, endpointConfig, listener);
    
    server = Undertow.builder()
        .setWorker(worker)
        .addHttpListener(config.port, "0.0.0.0")
        .setHandler(path().addPrefixPath(config.contextPath, websocket(manager)))
        .build();
        server.start();
  }
  
  @Override
  public void close() throws Exception {
    scanner.close();
    server.stop();
    worker.shutdown();
    worker.awaitTermination();
  }

  @Override
  public UndertowEndpointManager getEndpointManager() {
    return manager;
  }
  
  public static final class Factory implements WSServerFactory<UndertowEndpoint> {
    @Override public WSServer<UndertowEndpoint> create(WSServerConfig config,
                                                       WSEndpointListener<? super UndertowEndpoint> listener) throws Exception {
      return new UndertowServer(config, listener);
    }
  }
  
  public static WSServerFactory<UndertowEndpoint> factory() {
    return new Factory();
  }
}