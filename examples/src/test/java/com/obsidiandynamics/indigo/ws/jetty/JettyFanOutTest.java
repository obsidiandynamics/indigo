package com.obsidiandynamics.indigo.ws.jetty;

import static junit.framework.TestCase.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.*;
import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class JettyFanOutTest implements TestSupport {
  private final class ServerHarness {
    final AtomicInteger connected = new AtomicInteger();
    final AtomicInteger closed = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger sent = new AtomicInteger();
    
    final AtomicBoolean ping = new AtomicBoolean(true);
    
    final JettyServer server;
    
    private final JettyEndpointManager manager;
    private final WriteCallback writeCallback;
    
    ServerHarness(int port, int idleTimeout) throws Exception {
      final JettyMessageListener serverListener = new JettyMessageListener() {
        @Override public void onConnect(Session session) {
          log("s: connected: %s\n", session.getRemoteAddress());
          connected.incrementAndGet();
          if (idleTimeout != 0) Threads.asyncDaemon(() -> {
            while (ping.get()) {
              try {
                if (session.isOpen()) {
                  session.getRemote().sendPing(null);
                }
              } catch (WebSocketException e) {
                log("s: ping skipped\n");
                return;
              } catch (IOException e) {
                e.printStackTrace();
              }
              TestSupport.sleep(idleTimeout / 2);
            }
          }, "PingThread");
        }

        @Override public void onText(Session session, String message) {
          log("s: received: %s\n", message);
          received.incrementAndGet();
        }

        @Override
        public void onBinary(Session session, byte[] payload, int offset, int len) {
          log("s: received %d bytes\n", len);
          received.incrementAndGet();
        }
        
        @Override public void onClose(Session session, int statusCode, String reason) {
          log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
          closed.incrementAndGet();
          ping.set(false);
        }
        
        @Override public void onError(Session session, Throwable cause) {
          log("s: socket error\n");
          System.err.println("server socket error");
          cause.printStackTrace();
        }
      };
      
      manager = new JettyEndpointManager(idleTimeout, new JettyEndpointConfig() {{
        highWaterMark = Long.MAX_VALUE;
      }}, serverListener);
      server = new JettyServer(port, "/", manager);
      
      writeCallback = new WriteCallback() {
        @Override public void writeSuccess() {
          sent.incrementAndGet();
        }
        
        @Override public void writeFailed(Throwable x) {
          System.err.println("server write error");
          x.printStackTrace();
        }
      };
    }
    
    void broadcast(byte[] payload) {
      for (JettyEndpoint endpoint : manager.getEndpoints()) {
        endpoint.send(ByteBuffer.wrap(payload), writeCallback);
      }
    }
  }
  
  private final class ClientHarness {
    final AtomicBoolean connected = new AtomicBoolean();
    final AtomicBoolean closed = new AtomicBoolean();
    final AtomicInteger sent = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    
    final Session session;
    
    private final WriteCallback writeCallback;
    
    ClientHarness(HttpClient hc, int port, int idleTimeout, boolean echo) throws Exception {
      final JettyMessageListener clientListener = new JettyMessageListener() {
        @Override public void onConnect(Session session) {
          log("c: connected: %s\n", session.getRemoteAddress());
          connected.set(true);
        }

        @Override public void onText(Session session, String message) {
          log("c: received: %s\n", message);
          received.incrementAndGet();
          if (echo) {
            send(ByteBuffer.wrap(message.getBytes()));
          }
        }

        @Override
        public void onBinary(Session session, byte[] payload, int offset, int len) {
          log("c: received %d bytes\n", len);
          received.incrementAndGet();
          if (echo) {
            send(ByteBuffer.wrap(payload, offset, len));
          }
        }
        
        @Override public void onClose(Session session, int statusCode, String reason) {
          log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
          closed.set(true);
        }
        
        @Override public void onError(Session session, Throwable cause) {
          log("c: socket error\n");
          System.err.println("client socket error");
          cause.printStackTrace();
        }
      };
      
      final WebSocketClient client = new WebSocketClient(hc);
      client.setMaxIdleTimeout(idleTimeout);
      client.start();
      session = client.connect(JettyEndpoint.clientOf(new JettyEndpointConfig(), clientListener), URI.create("ws://localhost:" + port)).get();
      writeCallback = new WriteCallback() {
        @Override public void writeSuccess() {
          sent.incrementAndGet();
        }
        
        @Override public void writeFailed(Throwable x) {
          System.err.println("client write error");
          x.printStackTrace();
        }
      };
    }
    
    private void send(ByteBuffer payload) {
      session.getRemote().sendBytes(payload, writeCallback);
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
    test(100, 10, 0, false, 10);
  }
  
  private void test(int n, int m, int idleTimeout, boolean echo, int numBytes) throws Exception {
    final int port = 6667;
    final int httpClientThreads = 100;
    final boolean logTimings = true;
    final int sendThreads = 10;
    final int waitScale = 1 + n * m / 1_000_000;
    
    if (n % sendThreads != 0) throw new IllegalArgumentException("n must be a whole multiple of sendThreads");

    final ServerHarness server = new ServerHarness(port, idleTimeout);
    final List<ClientHarness> clients = new ArrayList<>(m);
    final HttpClient hc = new HttpClient();
    hc.setExecutor(new QueuedThreadPool(httpClientThreads));
    hc.start();
    for (int i = 0; i < m; i++) {
      clients.add(new ClientHarness(hc, port, idleTimeout, echo)); 
    }
    
    assertEquals(m, totalConnected(clients));

    final byte[] bytes = new byte[numBytes];
    new Random().nextBytes(bytes);
    final long start = System.currentTimeMillis();
    final int lim = n / sendThreads;
    ParallelJob.blocking(sendThreads, t -> {
      for (int i = 0; i < lim; i++) {
        server.broadcast(bytes);
      }
    }).run();

    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalReceived(clients) >= m * n);
    assertEquals(m * n, totalReceived(clients));
    
    assertEquals(m, server.connected.get());
    assertEquals(m * n, server.sent.get());
    
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
      client.session.close();
    }

    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.closed.get() == m);
    assertEquals(m, server.closed.get());
    assertEquals(m, totalClosed(clients));

    if (echo) {
      assertEquals(m * n, server.received.get());
    } else {
      assertEquals(0, server.received.get());
    }

    server.server.close();
  }
}
