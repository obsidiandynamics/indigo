package com.obsidiandynamics.indigo.ws;

import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.*;
import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public class WSServerEndpointManagerTest implements TestSupport {
  private WSServer server;

  @After
  public void teardown() throws Exception {
    server.close();
  }

  @Test
  public void test() throws Exception {
    final int port = 6667;
    final int n = 1;
    final AtomicBoolean serverConnected = new AtomicBoolean();
    final AtomicBoolean serverClosed = new AtomicBoolean();
    final AtomicInteger serverReceived = new AtomicInteger();
    final AtomicInteger serverSent = new AtomicInteger();
    final AtomicBoolean clientConnected = new AtomicBoolean();
    final AtomicBoolean clientClosed = new AtomicBoolean();
    final AtomicInteger clientSent = new AtomicInteger();
    final AtomicInteger clientReceived = new AtomicInteger();
    
    final AtomicBoolean ping = new AtomicBoolean(true);
    
    final MessageListener serverListener = new MessageListener() {
      @Override public void onConnect(Session session) {
        log("s: connected: %s\n", session.getRemoteAddress());
        serverConnected.set(true);
        Threads.asyncDaemon(() -> {
          while (ping.get()) {
            try {
              session.getRemote().sendPing(null);
            } catch (IOException e) {
              e.printStackTrace();
            }
            TestSupport.sleep(500);
          }
        }, "PingThread");
      }

      @Override public void onText(Session session, String message) {
        log("s: received: %s\n", message);
        serverReceived.incrementAndGet();
        session.getRemote().sendString("hello from server", new WriteCallback() {
          @Override public void writeSuccess() {
            serverSent.incrementAndGet();
          }
          
          @Override public void writeFailed(Throwable x) {
            x.printStackTrace();
          }
        });
      }
      
      @Override public void onClose(Session session, int statusCode, String reason) {
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        serverClosed.set(true);
      }
      
      @Override public void onError(Session session, Throwable cause) {
        log("s: socket error\n");
        cause.printStackTrace();
      }
    };
    
    server = new WSServer(port, "/", new EndpointManager(new EndpointConfig() {{
      idleTimeoutMillis = 1000;
    }}, serverListener));
    
    final MessageListener clientListener = new MessageListener() {
      @Override public void onConnect(Session session) {
        log("c: connected: %s\n", session.getRemoteAddress());
        clientConnected.set(true);
      }

      @Override public void onText(Session session, String message) {
        log("c: received: %s\n", message);
        clientReceived.incrementAndGet();
      }
      
      @Override public void onClose(Session session, int statusCode, String reason) {
        log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        clientClosed.set(true);
      }
      
      @Override public void onError(Session session, Throwable cause) {
        log("c: socket error\n");
        cause.printStackTrace();
      }
    };
    
    final WebSocketClient client = new WebSocketClient();
    client.setMaxIdleTimeout(1000);
    client.start();
    final Session clientSession = client.connect(Endpoint.clientOf(new EndpointConfig(), clientListener), URI.create("ws://localhost:" + port)).get();
    clientSession.getRemote().sendString("hello from client", new WriteCallback() {
      @Override public void writeSuccess() {
        clientSent.incrementAndGet();
      }
      
      @Override public void writeFailed(Throwable x) {
        x.printStackTrace();
      }
    });
    clientSession.close();
    
    await().atMost(10, TimeUnit.SECONDS).until(() -> serverClosed.get() && clientClosed.get());
    
    assertTrue(serverConnected.get());
    assertEquals(n, serverReceived.get());
    assertEquals(n, serverSent.get());
    assertTrue(serverClosed.get());
    
    assertTrue(clientConnected.get());
    assertEquals(n, clientSent.get());
    assertEquals(n, clientReceived.get());
    assertTrue(clientClosed.get());
  }
}
