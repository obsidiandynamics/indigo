package com.obsidiandynamics.indigo.ws;

import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.*;
import org.eclipse.jetty.websocket.server.*;
import org.eclipse.jetty.websocket.servlet.*;
import org.junit.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.adder.ws.*;
import com.obsidiandynamics.indigo.util.*;

public class WSServerTest implements TestSupport {
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
    final AtomicBoolean clientConnected = new AtomicBoolean();
    final AtomicBoolean clientClosed = new AtomicBoolean();
    final AtomicInteger clientReceived = new AtomicInteger();
    
    final AtomicBoolean ping = new AtomicBoolean(true);
    
    final WebSocketAdapter serverAdapter = new WebSocketAdapter() {
      @Override public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        log("s: connected: %s\n", sess.getRemoteAddress());
        serverConnected.set(true);
        Threads.asyncDaemon(() -> {
          while (ping.get()) {
            try {
              sess.getRemote().sendPing(null);
            } catch (IOException e) {
              e.printStackTrace();
            }
            TestSupport.sleep(500);
          }
        }, "PingThread");
      }

      @Override public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        log("s: received: %s\n", message);
        serverReceived.incrementAndGet();
        getSession().getRemote().sendStringByFuture("hello from server");
      }
      
      @Override public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        log("s: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        serverClosed.set(true);
      }
      
      @Override public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        log("s: socket error\n");
        cause.printStackTrace();
      }
    };
    
    server = new WSServer(port, "/", new WebSocketHandler() {
      @Override public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(1000);
        factory.setCreator(new WebSocketCreator() {
          @Override public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            return serverAdapter;
          }
        });
      }
    });
    
    final WebSocketAdapter clientAdapter = new WebSocketAdapter() {
      @Override public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        log("c: connected: %s\n", sess.getRemoteAddress());
        clientConnected.set(true);
      }

      @Override public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        log("c: received: %s\n", message);
        clientReceived.incrementAndGet();
      }
      
      @Override public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        log("c: disconnected: statusCode=%d, reason=%s\n", statusCode, reason);
        clientClosed.set(true);
      }
      
      @Override public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        log("c: socket error\n");
        cause.printStackTrace();
      }
    };
    
    final WebSocketClient client = new WebSocketClient();
    client.setMaxIdleTimeout(1000);
    client.start();
    final Session clientSession = client.connect(clientAdapter, URI.create("ws://localhost:" + port)).get();
    clientSession.getRemote().sendStringByFuture("hello from client");
    clientSession.close();
    
    await().atMost(10, TimeUnit.SECONDS).until(() -> serverClosed.get() && clientClosed.get());
    
    assertTrue(serverConnected.get());
    assertEquals(n, serverReceived.get());
    assertTrue(serverClosed.get());
    
    assertTrue(clientConnected.get());
    assertEquals(n, clientReceived.get());
    assertTrue(clientClosed.get());
  }
}
