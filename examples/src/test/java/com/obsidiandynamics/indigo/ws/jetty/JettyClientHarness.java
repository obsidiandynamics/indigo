package com.obsidiandynamics.indigo.ws.jetty;

import java.net.*;
import java.nio.*;

import org.eclipse.jetty.client.*;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ws.*;

public final class JettyClientHarness extends ClientHarness implements TestSupport {
  private final Session session;
  
  private final WriteCallback writeCallback;
  
  JettyClientHarness(HttpClient httpClient, int port, int idleTimeout, boolean echo) throws Exception {
    final JettyMessageListener clientListener = new JettyMessageListener() {
      @Override public void onConnect(JettyEndpoint endpoint, Session session) {
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
    
    final WebSocketClient client = new WebSocketClient(httpClient);
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
  
  @Override
  public void close() throws Exception {
    session.close();
  }
  
  public static ThrowingFactory<JettyClientHarness> factory(HttpClient httpClient, int port, int idleTimeout, boolean echo) {
    return () -> new JettyClientHarness(httpClient, port, idleTimeout, echo);
  }
}
