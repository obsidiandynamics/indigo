package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.util.*;

final class DefaultServerHarness<E extends WSEndpoint> extends ServerHarness<E> implements TestSupport {
  private final AtomicBoolean ping = new AtomicBoolean(true);
  private final WSServer<E> server;
  private final SendCallback writeCallback;
  
  DefaultServerHarness(WSServerConfig config, WSServerFactory<E> factory, ServerProgress progress) throws Exception {
    final EndpointListener<E> serverListener = new EndpointListener<E>() {
      @Override public void onConnect(E endpoint) {
        log("s: connected %s\n", endpoint.getRemoteAddress());
        connected.incrementAndGet();
        keepAlive(endpoint, ping, config.idleTimeoutMillis);
      }

      @Override public void onText(E endpoint, String message) {
        log("s: received: %s\n", message);
        received.incrementAndGet();
      }

      @Override
      public void onBinary(E endpoint, ByteBuffer message) {
        log("s: received %d bytes\n", message.limit());
        received.incrementAndGet();
      }
      
      @Override public void onClose(E endpoint, int statusCode, String reason) {
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        closed.incrementAndGet();
        ping.set(false);
      }
      
      @Override public void onError(E endpoint, Throwable cause) {
        log("s: socket error\n");
        System.err.println("server socket error");
        cause.printStackTrace();
      }
    };
    
    writeCallback = new SendCallback() {
      @Override public void onComplete(WSEndpoint endpoint) {
        final long s = sent.getAndIncrement();
        if (s % 1000 == 0) progress.update(DefaultServerHarness.this, s);
        if (WSFanOutTest.LOG_1K && s % 1000 == 0) System.out.format("s: confirmed %,d\n", s);
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
  public List<E> getEndpoints() {
    return new ArrayList<>(server.getEndpointManager().getEndpoints());
  }

  @Override
  public void broadcast(List<E> endpoints, byte[] payload) {
    for (E endpoint : endpoints) {
      endpoint.send(ByteBuffer.wrap(payload), writeCallback);
    }
  }

  @Override
  public void broadcast(List<E> endpoints, String payload) {
    for (E endpoint : endpoints) {
      endpoint.send(payload, writeCallback);
    }
  }

  @Override
  public void flush(List<E> endpoints) throws IOException {
    for (E endpoint : endpoints) {
      endpoint.flush();
    }
  }

  @Override
  public void sendPing(E endpoint) {
    endpoint.sendPing();
  }
}