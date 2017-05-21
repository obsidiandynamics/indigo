package com.obsidiandynamics.indigo.ws.undertow;

import static io.undertow.Handlers.*;

import org.xnio.*;

import io.undertow.*;
import io.undertow.websockets.*;

public final class UndertowServer implements AutoCloseable {
  private final Undertow server;
  
  public UndertowServer(int port, String contextPath, WebSocketConnectionCallback connCallback) throws Exception {
    final int ioThreads = Runtime.getRuntime().availableProcessors();
    final int coreWorkerThreads = 100;
    final int maxWorkerThreads = coreWorkerThreads * 100;
    
    final XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.builder()
                                                              .set(Options.WORKER_IO_THREADS, ioThreads)
                                                              .set(Options.WORKER_TASK_CORE_THREADS, coreWorkerThreads)
                                                              .set(Options.WORKER_TASK_MAX_THREADS, maxWorkerThreads)
                                                              .set(Options.TCP_NODELAY, true)
                                                              .getMap());
    
    server = Undertow.builder()
        .setWorker(worker)
        //.setSocketOption(Options.SEND_BUFFER, 100000)
        //.setServerOption(Options.CORK, true)
        .addHttpListener(port, "localhost")
        .setHandler(path().addPrefixPath(contextPath, websocket(connCallback)))
        .build();
        server.start();
  }
  
  @Override
  public void close() throws Exception {
    server.stop();
  }
}