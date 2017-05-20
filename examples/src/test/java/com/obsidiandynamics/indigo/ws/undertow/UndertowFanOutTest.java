package com.obsidiandynamics.indigo.ws.undertow;

import static junit.framework.TestCase.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.junit.*;
import org.xnio.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;
import com.obsidiandynamics.indigo.ws.fake.*;

import io.undertow.connector.*;
import io.undertow.server.*;
import io.undertow.websockets.client.*;
import io.undertow.websockets.core.*;

public final class UndertowFanOutTest implements TestSupport {
  private final class ServerHarness {
    final AtomicInteger connected = new AtomicInteger();
    final AtomicInteger closed = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger sent = new AtomicInteger();
    
    final AtomicBoolean ping = new AtomicBoolean(true);
    
    final UndertowServer server;
    
    private final UndertowEndpointManager manager;
    private final WebSocketCallback<Void> writeCallback;
    
    ServerHarness(int port, int idleTimeout) throws Exception {
      final UndertowMessageListener serverListener = new UndertowMessageListener() {
        @Override public void onConnect(UndertowEndpoint endpoint, WebSocketChannel channel) {
          log("s: connected %s\n", channel.getSourceAddress());
          connected.incrementAndGet();
        }

        @Override public void onText(WebSocketChannel channel, BufferedTextMessage message) {
          log("s: received: %s\n", message);
          received.incrementAndGet();
        }

        @Override
        public void onBinary(WebSocketChannel channel, BufferedBinaryMessage message) {
          final ByteBuffer buf = WebSockets.mergeBuffers(message.getData().getResource());
          log("s: received %d bytes\n", buf.limit());
          received.incrementAndGet();
        }
        
        @Override public void onClose(WebSocketChannel channel, int statusCode, String reason) {
          log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
          closed.incrementAndGet();
          ping.set(false);
        }
        
        @Override public void onError(WebSocketChannel channel, Throwable cause) {
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
    
    List<UndertowEndpoint> getEndpoints() {
      return new ArrayList<>(manager.getEndpoints());
    }
    
    void broadcast(List<UndertowEndpoint> endpoints, byte[] payload) {
      for (UndertowEndpoint endpoint : endpoints) {
        endpoint.send(ByteBuffer.wrap(payload), writeCallback);
      }
    }
    
    void flush(List<UndertowEndpoint> endpoints) {
      for (UndertowEndpoint endpoint : endpoints) {
        endpoint.flush();
      }
    }
  }
  
  private final class ClientHarness {
    final AtomicBoolean connected = new AtomicBoolean();
    final AtomicBoolean closed = new AtomicBoolean();
    final AtomicInteger sent = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    
    final WebSocketChannel channel;
    
    private final WebSocketCallback<Void> writeCallback;
    private final XnioWorker worker;
    
    ClientHarness(int port, int idleTimeout, boolean echo) throws Exception {
      final UndertowMessageListener clientListener = new UndertowMessageListener() {
        @Override public void onConnect(UndertowEndpoint endpoint, WebSocketChannel channel) {
          log("c: connected: %s\n", channel.getSourceAddress());
          connected.set(true);
        }

        @Override public void onText(WebSocketChannel channel, BufferedTextMessage message) {
          log("c: received: %s\n", message);
          received.incrementAndGet();
          if (echo) {
            send(ByteBuffer.wrap(message.getData().getBytes()));
          }
        }

        @Override
        public void onBinary(WebSocketChannel channel, BufferedBinaryMessage message) {
          log("c: received\n");
          received.incrementAndGet();
          if (echo) {
            send(WebSockets.mergeBuffers(message.getData().getResource()));
          }
        }
        
        @Override public void onClose(WebSocketChannel channel, int statusCode, String reason) {
          log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
          closed.set(true);
        }
        
        @Override public void onError(WebSocketChannel channel, Throwable cause) {
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
                                               .set(Options.CORK, true)
                                               .getMap());
      final ByteBufferPool pool = new DefaultByteBufferPool(false, 1024);
      channel = WebSocketClient.connectionBuilder(worker, pool, URI.create("ws://127.0.0.1:" + port + "/"))
          .connect().get();
      channel.getReceiveSetter().set(UndertowEndpoint.clientOf(channel, new UndertowEndpointConfig(), clientListener));
      channel.resumeReceives();
      
      writeCallback = new WebSocketCallback<Void>() {
        @Override
        public void complete(WebSocketChannel channel, Void context) {
          sent.incrementAndGet();
        }

        @Override
        public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
          System.err.println("client write error");
          throwable.printStackTrace();
        }
      };
    }
    
    private void send(ByteBuffer payload) {
      WebSockets.sendBinary(payload, channel, writeCallback);
    }
    
    void close() throws IOException {
      channel.sendClose();
      Threads.asyncDaemon(() -> {
        TestSupport.sleep(1000);
        worker.shutdown();
      }, "WorkerTerminator");
    }
  }
  
  private static int totalConnected(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.connected.get() ? 1 : 0).sum();
  }
  
  private static int totalClosed(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.closed.get() ? 1 : 0).sum();
  }
  
  private static int totalReceived(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.received.get()).sum();
  }
  
  private static int totalSent(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.sent.get()).sum();
  }
  
  @Test
  public void test() throws Exception {
    test(1, 1, 0, false, 10, 1);
  }
  
  private void test(int n, int m, int idleTimeout, boolean echo, int numBytes, int cycles) throws Exception {
    for (int i = 0; i < cycles; i++) {
      test(n, m, idleTimeout, echo, numBytes);
    }
  }
  
  private void test(int n, int m, int idleTimeout, boolean echo, int numBytes) throws Exception {
    final int port = 6667;
    final boolean logTimings = true;
    final int sendThreads = 1;
    final int waitScale = 1 + n * m / 1_000_000;
    
    final ServerHarness server = new ServerHarness(port, idleTimeout);
    final List<ClientHarness> clients = new ArrayList<>(m);
    
    for (int i = 0; i < m; i++) {
      clients.add(new ClientHarness(port, idleTimeout, echo)); 
//      new FakeClient("/", port, numBytes, new FakeClientCallback() {
//        @Override public void connected() {
//          log("fc: connected\n");
//        }
//
//        @Override public void disconnected() {
//          log("fc: disconnected\n");
//        }
//
//        @Override
//        public void received(int messages) {
//          log("fc: received %d\n", messages);
//        }
//      });
    }
    
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.connected.get() == m);

    assertEquals(m, server.connected.get());
    assertEquals(m, totalConnected(clients));

    final byte[] bytes = new byte[numBytes];
    new Random().nextBytes(bytes);
    final long start = System.currentTimeMillis();
    
    final List<UndertowEndpoint> endpoints = server.getEndpoints();
    ParallelJob.blockingSlice(endpoints, sendThreads, sublist -> {
      for (int i = 0; i < n; i++) {
        server.broadcast(sublist, bytes);
      }
      for (int i = 0; i < n; i++) {
        server.flush(sublist);
      }
    }).run();

    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.sent.get() >= m * n);
    assertEquals(m * n, server.sent.get());
    
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalReceived(clients) >= m * n);
    assertEquals(m * n, totalReceived(clients));
    
    if (echo) {
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalSent(clients) >= m * n);
      assertEquals(m * n, totalSent(clients));
    } else {
      assertEquals(0, totalSent(clients));
    }
    
    if (logTimings) {
      final long tookMillis = System.currentTimeMillis() - start;
      final float ratePerSec = 1000f * n * m / tookMillis * (echo ? 2 : 1);
      final float bandwidthMpbs = ratePerSec * numBytes / (1 << 20);
      LOG_STREAM.format("took %,d ms, %,.0f/s, %,.1f Mb/s (%d threads active)\n", 
                        tookMillis, ratePerSec, bandwidthMpbs, Thread.activeCount());
    }

    for (ClientHarness client : clients) {
      client.close();
    }
    
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.closed.get() == m);
    assertEquals(m, server.closed.get());

    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalClosed(clients) == m);
    assertEquals(m, totalClosed(clients));

    if (echo) {
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.received.get() == m * n);
      assertEquals(m * n, server.received.get());
    } else {
      assertEquals(0, server.received.get());
    }

    server.server.close();
  }
}
