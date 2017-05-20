package com.obsidiandynamics.indigo.ws.undertow;

import static io.undertow.Handlers.*;

import io.undertow.*;
import io.undertow.websockets.*;

public final class UndertowServer implements AutoCloseable {
  private final Undertow server;
  
  public UndertowServer(int port, String contextPath, WebSocketConnectionCallback connCallback) throws Exception {
    server = Undertow.builder()
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