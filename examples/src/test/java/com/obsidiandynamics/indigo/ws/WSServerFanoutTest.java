package com.obsidiandynamics.indigo.ws;

import static junit.framework.TestCase.*;

import java.io.*;
import java.net.*;
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

public final class WSServerFanoutTest implements TestSupport {
  private final class ServerHarness {
    final AtomicInteger connected = new AtomicInteger();
    final AtomicInteger closed = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger sent = new AtomicInteger();
    
    final AtomicBoolean ping = new AtomicBoolean(true);
    
    final WSServer server;
    
    private final EndpointManager manager;
    private final WriteCallback writeCallback;
    
    ServerHarness(int port, int idleTimeout) throws Exception {
      final MessageListener serverListener = new MessageListener() {
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
      
      server = new WSServer(port, "/", manager = new EndpointManager(idleTimeout, new EndpointConfig(), serverListener));
      
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
    
    void broadcast(String payload) {
      for (Endpoint endpoint : manager.getEndpoints()) {
        endpoint.send(payload, writeCallback);
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
      final MessageListener clientListener = new MessageListener() {
        @Override public void onConnect(Session session) {
          log("c: connected: %s\n", session.getRemoteAddress());
          connected.set(true);
        }

        @Override public void onText(Session session, String message) {
          log("c: received: %s\n", message);
          received.incrementAndGet();
          if (echo) {
            send("hello from client");
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
      session = client.connect(Endpoint.clientOf(new EndpointConfig(), clientListener), URI.create("ws://localhost:" + port)).get();
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
    
    private void send(String payload) {
      session.getRemote().sendString(payload, writeCallback);
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
    test(10, 10, 0, false);
  }
  
  private void test(int n, int m, int idleTimeout, boolean echo) throws Exception {
    final int port = 6667;
    final int httpClientThreads = 100;
    final boolean logTimings = true;
    final int waitScale = 1 + n * m / 1_000_000;

    final ServerHarness server = new ServerHarness(port, idleTimeout);
    final List<ClientHarness> clients = new ArrayList<>(m);
    final HttpClient hc = new HttpClient();
    hc.setExecutor(new QueuedThreadPool(httpClientThreads));
    hc.start();
    for (int i = 0; i < m; i++) {
      clients.add(new ClientHarness(hc, port, idleTimeout, echo)); 
    }
    
    assertEquals(m, totalConnected(clients));
    
    final long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      server.broadcast("hello from server");
    }
    
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
      final long took = System.currentTimeMillis() - start;
      final float rate = 1000f * n * m / took;
      LOG_STREAM.format("took %,d ms, %,.0f/s (%d threads active)\n", took, rate, Thread.activeCount());
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
