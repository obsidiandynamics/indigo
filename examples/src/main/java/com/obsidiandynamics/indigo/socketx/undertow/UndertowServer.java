package com.obsidiandynamics.indigo.socketx.undertow;

import static io.undertow.Handlers.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.socketx.*;

import io.undertow.*;

public final class UndertowServer implements XServer<UndertowEndpoint> {
  private final Undertow server;
  private final UndertowEndpointManager manager;
  private final XnioWorker worker;
  private final XEndpointScanner<UndertowEndpoint> scanner;
  
  private UndertowServer(XServerConfig config,
                         XEndpointListener<? super UndertowEndpoint> listener) throws Exception {
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
    
    scanner = new XEndpointScanner<>(config.scanIntervalMillis, config.pingIntervalMillis);
    manager = new UndertowEndpointManager(scanner, config.idleTimeoutMillis, config.endpointConfig, listener);
    
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
  
  public static final class Factory implements XServerFactory<UndertowEndpoint> {
    @Override public XServer<UndertowEndpoint> create(XServerConfig config,
                                                       XEndpointListener<? super UndertowEndpoint> listener) throws Exception {
      return new UndertowServer(config, listener);
    }
  }
  
  public static XServerFactory<UndertowEndpoint> factory() {
    return new Factory();
  }
}