package com.obsidiandynamics.indigo.ws;

import static junit.framework.TestCase.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.*;
import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public final class WSServerFanoutTest implements TestSupport {
  @Test
  public void test() throws Exception {
    test(1000, 1, 1000);
  }
  
  private final class ServerHarness {
    final AtomicBoolean connected = new AtomicBoolean();
    final AtomicBoolean closed = new AtomicBoolean();
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger sent = new AtomicInteger();
    
    final AtomicBoolean ping = new AtomicBoolean(true);
    
    final WSServer server;
    
    ServerHarness(int port, int idleTimeout) throws Exception {
      final MessageListener serverListener = new MessageListener() {
        @Override public void onConnect(Session session) {
          log("s: connected: %s\n", session.getRemoteAddress());
          connected.set(true);
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
          session.getRemote().sendString("hello from server", new WriteCallback() {
            @Override public void writeSuccess() {
              sent.incrementAndGet();
            }
            
            @Override public void writeFailed(Throwable x) {
              System.err.println("server write error");
              x.printStackTrace();
            }
          });
        }
        
        @Override public void onClose(Session session, int statusCode, String reason) {
          log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
          closed.set(true);
          ping.set(false);
        }
        
        @Override public void onError(Session session, Throwable cause) {
          log("s: socket error\n");
          System.err.println("server socket error");
          cause.printStackTrace();
        }
      };
      
      server = new WSServer(port, "/", new EndpointManager(idleTimeout, new EndpointConfig(), serverListener));
    }
  }
  
  private final class ClientHarness {
    final AtomicBoolean connected = new AtomicBoolean();
    final AtomicBoolean closed = new AtomicBoolean();
    final AtomicInteger sent = new AtomicInteger();
    final AtomicInteger received = new AtomicInteger();
    
    final Session session;
    
    private final WriteCallback clientWriteCallback;
    
    ClientHarness(int port, int idleTimeout) throws Exception {
      final MessageListener clientListener = new MessageListener() {
        @Override public void onConnect(Session session) {
          log("c: connected: %s\n", session.getRemoteAddress());
          connected.set(true);
        }

        @Override public void onText(Session session, String message) {
          log("c: received: %s\n", message);
          received.incrementAndGet();
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
      
      final WebSocketClient client = new WebSocketClient();
      client.setMaxIdleTimeout(idleTimeout);
      client.start();
      session = client.connect(Endpoint.clientOf(new EndpointConfig(), clientListener), URI.create("ws://localhost:" + port)).get();
      clientWriteCallback = new WriteCallback() {
        @Override public void writeSuccess() {
          sent.incrementAndGet();
        }
        
        @Override public void writeFailed(Throwable x) {
          System.err.println("client write error");
          x.printStackTrace();
        }
      };
    }
    
    void send(String payload) {
      session.getRemote().sendString(payload, clientWriteCallback);
    }
  }
  
  private void test(int n, int clients, int idleTimeout) throws Exception {
    final int port = 6667;
    final boolean logTimings = true;

    final ServerHarness server = new ServerHarness(port, idleTimeout);
    
    final ClientHarness client = new ClientHarness(port, idleTimeout);
    
    final long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      client.send("hello from client");
    }
    
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> client.received.get() == n);
    
    client.session.close();
    
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> server.closed.get() && client.closed.get());
    if (logTimings) LOG_STREAM.format("took %d ms\n", System.currentTimeMillis() - start);
    
    assertTrue(server.connected.get());
    assertEquals(n, server.received.get());
    assertEquals(n, server.sent.get());
    assertTrue(server.closed.get());
    
    assertTrue(client.connected.get());
    assertEquals(n, client.sent.get());
    assertEquals(n, client.received.get());
    assertTrue(client.closed.get());
    
    server.server.close();
  }
}
