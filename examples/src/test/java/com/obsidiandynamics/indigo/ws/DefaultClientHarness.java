package com.obsidiandynamics.indigo.ws;

import java.net.*;
import java.nio.*;

import com.obsidiandynamics.indigo.*;

public final class DefaultClientHarness<E extends WSEndpoint> extends ClientHarness<E> implements TestSupport {
  private final SendCallback writeCallback;
  
  private final WSEndpoint endpoint;
  
  DefaultClientHarness(WSClient<E> client, int port, boolean echo) throws Exception {
    final EndpointListener<E> clientListener = new EndpointListener<E>() {
      @Override public void onConnect(E endpoint) {
        log("c: connected: %s\n", endpoint.getRemoteAddress());
        connected.set(true);
      }

      @Override public void onText(E endpoint, String message) {
        log("c: received: %s\n", message);
        received.incrementAndGet();
        if (echo) {
          send(ByteBuffer.wrap(message.getBytes()));
        }
      }

      @Override public void onBinary(E endpoint, ByteBuffer message) {
        log("c: received\n");
        final long r = received.incrementAndGet();
        if (WSFanOutTest.LOG_1K && r % 1000 == 0) System.out.println("c: received " + received);
        if (echo) {
          send(message);
        }
      }
      
      @Override public void onClose(E endpoint, int statusCode, String reason) {
        log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        closed.set(true);
      }
      
      @Override public void onError(E endpoint, Throwable cause) {
        log("c: socket error\n");
        System.err.println("client socket error");
        cause.printStackTrace();
      }
    };
    
    endpoint = client.connect(URI.create("ws://127.0.0.1:" + port + "/"), clientListener);
    
    writeCallback = new SendCallback() {
      @Override public void onComplete(WSEndpoint endpoint) {
        sent.incrementAndGet();
      }

      @Override public void onError(WSEndpoint endpoint, Throwable throwable) {
        System.err.println("client write error");
        throwable.printStackTrace();
      }
    };
  }
  
  private void send(ByteBuffer payload) {
    endpoint.send(payload, writeCallback);
  }
  
  @Override
  public void close() throws Exception {
    endpoint.close();
  }
}
