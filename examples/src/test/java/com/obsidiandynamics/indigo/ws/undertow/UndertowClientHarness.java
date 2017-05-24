package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.net.*;
import java.nio.*;

import org.xnio.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

import io.undertow.connector.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.core.*;

public final class UndertowClientHarness extends ClientHarness implements TestSupport {
  private final WebSocketChannel channel;
  
  private final WebSocketCallback<Void> writeCallback;
  
  UndertowClientHarness(XnioWorker worker, int port, int idleTimeout, int bufferSize, boolean echo) throws Exception {
    final WSListener<UndertowEndpoint> clientListener = new WSListener<UndertowEndpoint>() {
      @Override public void onConnect(UndertowEndpoint endpoint) {
        log("c: connected: %s\n", channel.getSourceAddress());
        connected.set(true);
      }

      @Override public void onText(UndertowEndpoint endpoint, String message) {
        log("c: received: %s\n", message);
        received.incrementAndGet();
        if (echo) {
          send(ByteBuffer.wrap(message.getBytes()));
        }
      }

      @Override public void onBinary(UndertowEndpoint endpoint, ByteBuffer message) {
        log("c: received\n");
        final int r = received.incrementAndGet();
        if (WSFanOutTest.LOG_1K && r % 1000 == 0) System.out.println("c: received " + received);
        if (echo) {
          send(message);
        }
      }
      
      @Override public void onClose(UndertowEndpoint endpoint, int statusCode, String reason) {
        log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        closed.set(true);
      }
      
      @Override public void onError(UndertowEndpoint endpoint, Throwable cause) {
        log("c: socket error\n");
        System.err.println("client socket error");
        cause.printStackTrace();
      }
    };
    
    final ByteBufferPool pool = new DefaultByteBufferPool(false, bufferSize);
    channel = WebSocketClient.connectionBuilder(worker, pool, URI.create("ws://127.0.0.1:" + port + "/"))
        .connect().get();
    channel.getReceiveSetter().set(UndertowEndpoint.clientOf(channel, new UndertowEndpointConfig(), clientListener));
    channel.resumeReceives();
    
    writeCallback = new WebSocketCallback<Void>() {
      @Override public void complete(WebSocketChannel channel, Void context) {
        sent.incrementAndGet();
      }

      @Override public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
        System.err.println("client write error");
        throwable.printStackTrace();
      }
    };
  }
  
  private void send(ByteBuffer payload) {
    WebSockets.sendBinary(payload, channel, writeCallback);
  }
  
  @Override
  public void close() throws IOException {
    channel.sendClose();
  }
  
  public static ThrowingSupplier<UndertowClientHarness> factory(XnioWorker worker, int port, int idleTimeout, int bufferSize, boolean echo) {
    return () -> new UndertowClientHarness(worker, port, idleTimeout, bufferSize, echo);
  }
}
