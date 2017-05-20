package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.WebSocketException;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ws.*;

import io.undertow.websockets.core.*;

public final class UndertowServerHarness extends ServerHarness<UndertowEndpoint> implements TestSupport {
  private final AtomicBoolean ping = new AtomicBoolean(true);
  
  private final UndertowServer server;
  
  private final UndertowEndpointManager manager;
  private final WebSocketCallback<Void> writeCallback;

  UndertowServerHarness(int port, int idleTimeout) throws Exception {
    final WSListener<UndertowEndpoint> serverListener = new WSListener<UndertowEndpoint>() {
      @Override public void onConnect(UndertowEndpoint endpoint) {
        log("s: connected %s\n", endpoint.getChannel().getSourceAddress());
        connected.incrementAndGet();
        keepAlive(endpoint, ping, idleTimeout);
      }

      @Override public void onText(UndertowEndpoint endpoint, String message) {
        log("s: received: %s\n", message);
        received.incrementAndGet();
      }

      @Override
      public void onBinary(UndertowEndpoint endpoint, ByteBuffer message) {
        log("s: received %d bytes\n", message.limit());
        received.incrementAndGet();
      }
      
      @Override public void onClose(UndertowEndpoint endpoint, int statusCode, String reason) {
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        closed.incrementAndGet();
        ping.set(false);
      }
      
      @Override public void onError(UndertowEndpoint endpoint, Throwable cause) {
        log("s: socket error\n");
        System.err.println("server socket error");
        cause.printStackTrace();
      }
    };
    
    manager = new UndertowEndpointManager(new UndertowEndpointConfig() {{
      highWaterMark = Long.MAX_VALUE;
    }}, serverListener);
    server = new UndertowServer(port, "/", manager);
    
    writeCallback = new WebSocketCallback<Void>() {
      @Override
      public void complete(WebSocketChannel channel, Void context) {
        sent.incrementAndGet();
      }

      @Override
      public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
        System.err.println("server write error");
        throwable.printStackTrace();
      }
    };
  }
  
  @Override
  public List<UndertowEndpoint> getEndpoints() {
    return new ArrayList<>(manager.getEndpoints());
  }

  @Override
  public void broadcast(List<UndertowEndpoint> endpoints, byte[] payload) {
    for (UndertowEndpoint endpoint : endpoints) {
      endpoint.send(ByteBuffer.wrap(payload), writeCallback);
    }
  }

  @Override
  public void flush(List<UndertowEndpoint> endpoints) {
    for (UndertowEndpoint endpoint : endpoints) {
      endpoint.flush();
    }
  }

  @Override
  public void close() throws Exception {
    server.close();
  }
  
  public static ThrowingFactory<UndertowServerHarness> factory(int port, int idleTimeout) {
    return () -> new UndertowServerHarness(port, idleTimeout);
  }

  @Override
  public void sendPong(UndertowEndpoint endpoint) throws IOException {
    try {
      endpoint.sendPong();
    } catch (WebSocketException e) {
      log("ping skipped\n");
      return;
    }
  }
}
