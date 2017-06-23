package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.util.*;

final class DefaultServerHarness extends ServerHarness implements TestSupport {
  private final AtomicBoolean ping = new AtomicBoolean(true);
  private final WSServer<WSEndpoint> server;
  private final SendCallback writeCallback;
  
  DefaultServerHarness(WSServerConfig config, WSServerFactory<WSEndpoint> factory, ServerProgress progress) throws Exception {
    final WSEndpointListener<WSEndpoint> serverListener = new WSEndpointListener<WSEndpoint>() {
      @Override public void onConnect(WSEndpoint endpoint) {
        log("s: connected %s\n", endpoint.getRemoteAddress());
        connected.incrementAndGet();
        keepAlive(endpoint, ping, config.idleTimeoutMillis);
      }

      @Override public void onText(WSEndpoint endpoint, String message) {
        log("s: received: %s\n", message);
        received.incrementAndGet();
      }

      @Override public void onBinary(WSEndpoint endpoint, ByteBuffer message) {
        log("s: received %d bytes\n", message.limit());
        received.incrementAndGet();
      }
      
      @Override public void onDisconnect(WSEndpoint endpoint, int statusCode, String reason) {
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
      }
      
      @Override public void onError(WSEndpoint endpoint, Throwable cause) {
        log("s: socket error\n");
        System.err.println("server socket error");
        cause.printStackTrace();
      }

      @Override public void onClose(WSEndpoint endpoint) {
        log("s: closed\n");
        closed.incrementAndGet();
        ping.set(false);
      }

      @Override public void onPing(ByteBuffer data) {
        log("s: ping\n");
      }

      @Override public void onPong(ByteBuffer data) {
        log("s: pong\n");
      }
    };
    
    writeCallback = new SendCallback() {
      @Override public void onComplete(WSEndpoint endpoint) {
        final long s = sent.getAndIncrement();
        if (s % 1000 == 0) progress.update(DefaultServerHarness.this, s);
      }

      @Override public void onError(WSEndpoint endpoint, Throwable cause) {
        System.err.println("server write error");
        cause.printStackTrace();
      }
    };
    server = factory.create(config, serverListener);
  }

  @Override
  public void close() throws Exception {
    server.close();
  }

  @Override
  public List<WSEndpoint> getEndpoints() {
    return new ArrayList<>(server.getEndpointManager().getEndpoints());
  }

  @Override
  public void broadcast(List<WSEndpoint> endpoints, byte[] payload) {
    for (WSEndpoint endpoint : endpoints) {
      endpoint.send(ByteBuffer.wrap(payload), writeCallback);
    }
  }

  @Override
  public void broadcast(List<WSEndpoint> endpoints, String payload) {
    for (WSEndpoint endpoint : endpoints) {
      endpoint.send(payload, writeCallback);
    }
  }

  @Override
  public void flush(List<WSEndpoint> endpoints) throws IOException {
    for (WSEndpoint endpoint : endpoints) {
      endpoint.flush();
    }
  }

  @Override
  public void sendPing(WSEndpoint endpoint) {
    endpoint.sendPing();
  }
}
