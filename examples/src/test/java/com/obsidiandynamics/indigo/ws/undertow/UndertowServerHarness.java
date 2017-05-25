package com.obsidiandynamics.indigo.ws.undertow;

import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class UndertowServerHarness extends ServerHarness<UndertowEndpoint> implements TestSupport {
  private final AtomicBoolean ping = new AtomicBoolean(true);
  
  private final UndertowServer server;
  
  private final UndertowEndpointManager manager;
  private final SendCallback<UndertowEndpoint> writeCallback;

  UndertowServerHarness(int port, int idleTimeout) throws Exception {
    final EndpointListener<UndertowEndpoint> serverListener = new EndpointListener<UndertowEndpoint>() {
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
    
    writeCallback = new SendCallback<UndertowEndpoint>() {
      @Override public void onComplete(UndertowEndpoint endpoint) {
        final int s = sent.incrementAndGet();
        if (WSFanOutTest.LOG_1K && s % 1000 == 0) System.out.println("s: confirmed " + s);
      }

      @Override public void onError(UndertowEndpoint endpoint, Throwable throwable) {
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

  @Override
  public void sendPing(UndertowEndpoint endpoint) {
    endpoint.sendPing();
  }
  
  public static ThrowingSupplier<UndertowServerHarness> factory(int port, int idleTimeout) {
    return () -> new UndertowServerHarness(port, idleTimeout);
  }
}
