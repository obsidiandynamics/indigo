package com.obsidiandynamics.indigo.ws.jetty;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class JettyServerHarness extends ServerHarness<JettyEndpoint> implements TestSupport {
  private final AtomicBoolean ping = new AtomicBoolean(true);
  
  private final JettyServer server;
  
  private final JettyEndpointManager manager;
  private final SendCallback<JettyEndpoint> writeCallback;
  
  JettyServerHarness(int port, int idleTimeout) throws Exception {
    final EndpointListener<JettyEndpoint> serverListener = new EndpointListener<JettyEndpoint>() {
      @Override public void onConnect(JettyEndpoint endpoint) {
        log("s: connected: %s\n", endpoint.getRemote().getInetSocketAddress());
        connected.incrementAndGet();
        keepAlive(endpoint, ping, idleTimeout);
      }

      @Override public void onText(JettyEndpoint endpoint, String message) {
        log("s: received: %s\n", message);
        received.incrementAndGet();
      }

      @Override
      public void onBinary(JettyEndpoint endpoint, ByteBuffer message) {
        log("s: received %d bytes\n", message.limit());
        received.incrementAndGet();
      }
      
      @Override public void onClose(JettyEndpoint endpoint, int statusCode, String reason) {
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        closed.incrementAndGet();
        ping.set(false);
      }
      
      @Override public void onError(JettyEndpoint endpoint, Throwable cause) {
        log("s: socket error\n");
        System.err.println("server socket error");
        cause.printStackTrace();
      }
    };
    
    manager = new JettyEndpointManager(idleTimeout, new JettyEndpointConfig() {{
      highWaterMark = Long.MAX_VALUE;
    }}, serverListener);
    server = new JettyServer(port, "/", manager);
    
    writeCallback = new SendCallback<JettyEndpoint>() {
      @Override public void onComplete(JettyEndpoint endpoint) {
        final long s = sent.incrementAndGet();
        if (WSFanOutTest.LOG_1K && s % 1000 == 0) System.out.println("s: confirmed " + s);
      }

      @Override public void onError(JettyEndpoint endpoint, Throwable throwable) {
        System.err.println("server write error");
        throwable.printStackTrace();
      }
    };
  }

  @Override
  public List<JettyEndpoint> getEndpoints() {
    return new ArrayList<>(manager.getEndpoints());
  }

  @Override
  public void broadcast(List<JettyEndpoint> endpoints, byte[] payload) {
    for (JettyEndpoint endpoint : endpoints) {
      endpoint.send(ByteBuffer.wrap(payload), writeCallback);
    }
  }

  @Override
  public void flush(List<JettyEndpoint> endpoints) throws IOException {
    for (JettyEndpoint endpoint : endpoints) {
      endpoint.flush();
    }
  }

  @Override
  public void close() throws Exception {
    server.close();
  }

  @Override
  public void sendPing(JettyEndpoint endpoint) {
    endpoint.sendPing();
  }
  
  public static ThrowingSupplier<JettyServerHarness> factory(int port, int idleTimeout) {
    return () -> new JettyServerHarness(port, idleTimeout);
  }
}
