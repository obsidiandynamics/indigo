package com.obsidiandynamics.indigo.ws.netty;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class NettyServerHarness extends ServerHarness<NettyEndpoint> implements TestSupport {
  private final AtomicBoolean ping = new AtomicBoolean(true);
  
  private final NettyServer server;
  
  private final NettyEndpointManager manager;
  private final SendCallback<NettyEndpoint> writeCallback;
  
  NettyServerHarness(int port, int idleTimeout) throws Exception {
    final EndpointListener<NettyEndpoint> serverListener = new EndpointListener<NettyEndpoint>() {
      @Override public void onConnect(NettyEndpoint endpoint) {
        log("s: connected: %s\n", endpoint.getHandlerContext().channel().remoteAddress());
        connected.incrementAndGet();
        keepAlive(endpoint, ping, idleTimeout);
      }

      @Override public void onText(NettyEndpoint endpoint, String message) {
        log("s: received: %s\n", message);
        received.incrementAndGet();
      }

      @Override
      public void onBinary(NettyEndpoint endpoint, ByteBuffer message) {
        log("s: received %d bytes\n", message.limit());
        received.incrementAndGet();
      }
      
      @Override public void onClose(NettyEndpoint endpoint, int statusCode, String reason) {
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        closed.incrementAndGet();
        ping.set(false);
      }
      
      @Override public void onError(NettyEndpoint endpoint, Throwable cause) {
        log("s: socket error\n");
        System.err.println("server socket error");
        cause.printStackTrace();
      }
    };
    
    manager = new NettyEndpointManager(new NettyEndpointConfig() {{
      highWaterMark = Long.MAX_VALUE;
    }}, serverListener);
    server = new NettyServer(port, "/", manager);
    
    writeCallback = new SendCallback<NettyEndpoint>() {
      @Override public void onComplete(NettyEndpoint endpoint) {
        final long s = sent.incrementAndGet();
        if (WSFanOutTest.LOG_1K && s % 1000 == 0) System.out.println("s: confirmed " + s);
      }

      @Override public void onError(NettyEndpoint endpoint, Throwable throwable) {
        System.err.println("server write error");
        throwable.printStackTrace();
      }
    };
  }

  @Override
  public List<NettyEndpoint> getEndpoints() {
    return new ArrayList<>(manager.getEndpoints());
  }

  @Override
  public void broadcast(List<NettyEndpoint> endpoints, byte[] payload) {
    for (NettyEndpoint endpoint : endpoints) {
      endpoint.send(ByteBuffer.wrap(payload), writeCallback);
    }
  }

  @Override
  public void flush(List<NettyEndpoint> endpoints) throws IOException {
    for (NettyEndpoint endpoint : endpoints) {
      endpoint.flush();
    }
  }

  @Override
  public void close() throws Exception {
    server.close();
  }

  @Override
  public void sendPing(NettyEndpoint endpoint) {
    endpoint.sendPing();
  }
  
  public static ThrowingSupplier<NettyServerHarness> factory(int port, int idleTimeout) {
    return () -> new NettyServerHarness(port, idleTimeout);
  }
}
