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
  private final XnioWorker worker;
  
  UndertowClientHarness(int port, int idleTimeout, boolean echo) throws Exception {
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

      @Override
      public void onBinary(UndertowEndpoint endpoint, ByteBuffer message) {
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
    
    worker = Xnio.getInstance().createWorker(OptionMap.builder()
                                             .set(Options.WORKER_IO_THREADS, 1)
                                             .set(Options.CONNECTION_HIGH_WATER, 1000000)
                                             .set(Options.CONNECTION_LOW_WATER, 1000000)
                                             .set(Options.WORKER_TASK_CORE_THREADS, 1)
                                             .set(Options.WORKER_TASK_MAX_THREADS, 1)
                                             .set(Options.TCP_NODELAY, true)
                                             //.set(Options.CORK, true)
                                             .getMap());
    final ByteBufferPool pool = new DefaultByteBufferPool(false, 8192);
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
    Threads.asyncDaemon(() -> {
      TestSupport.sleep(1000);
      worker.shutdown();
    }, "WorkerTerminator");
  }
  
  public static ThrowingFactory<UndertowClientHarness> factory(int port, int idleTimeout, boolean echo) {
    return () -> new UndertowClientHarness(port, idleTimeout, echo);
  }
}
