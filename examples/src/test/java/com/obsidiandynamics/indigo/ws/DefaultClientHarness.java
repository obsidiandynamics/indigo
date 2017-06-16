package com.obsidiandynamics.indigo.ws;

import java.net.*;
import java.nio.*;

import com.obsidiandynamics.indigo.util.*;

public final class DefaultClientHarness extends ClientHarness implements TestSupport {
  private final SendCallback writeCallback;
  
  private final WSEndpoint endpoint;
  
  DefaultClientHarness(WSClient<?> client, int port, boolean echo) throws Exception {
    final WSEndpointListener<WSEndpoint> clientListener = new WSEndpointListener<WSEndpoint>() {
      @Override public void onConnect(WSEndpoint endpoint) {
        log("c: connected: %s\n", endpoint.getRemoteAddress());
        connected.set(true);
      }

      @Override public void onText(WSEndpoint endpoint, String message) {
        log("c: received: %s\n", message);
        received.incrementAndGet();
        if (echo) {
          send(ByteBuffer.wrap(message.getBytes()));
        }
      }

      @Override public void onBinary(WSEndpoint endpoint, ByteBuffer message) {
        log("c: received\n");
        received.incrementAndGet();
        if (echo) {
          send(message);
        }
      }
      
      @Override public void onDisconnect(WSEndpoint endpoint, int statusCode, String reason) {
        log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
      }
      
      @Override public void onError(WSEndpoint endpoint, Throwable cause) {
        log("c: socket error\n");
        System.err.println("client socket error");
        cause.printStackTrace();
      }

      @Override public void onClose(WSEndpoint endpoint) {
        log("c: closed\n");
        closed.set(true);
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
