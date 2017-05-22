package com.obsidiandynamics.indigo.ws.jetty;

import java.io.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.*;

import com.obsidiandynamics.indigo.ws.*;

public final class JettyEndpoint extends WebSocketAdapter implements WSEndpoint {
  private final JettyEndpointManager manager;
  
  private final AtomicLong backlog = new AtomicLong();

  JettyEndpoint(JettyEndpointManager manager) {
    this.manager = manager;
  }
  
  public static JettyEndpoint clientOf(JettyEndpointConfig config, WSListener<JettyEndpoint> listener) {
    return new JettyEndpointManager(0, config, listener).createEndpoint();
  }
  
  @Override 
  public void onWebSocketConnect(Session session) {
    super.onWebSocketConnect(session);
    manager.getListener().onConnect(this);
  }

  @Override 
  public void onWebSocketText(String message) {
    super.onWebSocketText(message);
    manager.getListener().onText(this, message);
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int offset, int len) {
    super.onWebSocketBinary(payload, offset, len);
    final ByteBuffer message = ByteBuffer.wrap(payload, offset, len);
    manager.getListener().onBinary(this, message);
  }
  
  @Override 
  public void onWebSocketClose(int statusCode, String reason) {
    super.onWebSocketClose(statusCode, reason);
    manager.getListener().onClose(this, statusCode, reason);
  }
  
  @Override 
  public void onWebSocketError(Throwable cause) {
    super.onWebSocketError(cause);
    manager.getListener().onError(this, cause);
  }
  
  public void send(String payload, WriteCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      getRemote().sendString(payload, wrapCallback(callback));
    }
  }
  
  public void send(ByteBuffer payload, WriteCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      getRemote().sendBytes(payload, wrapCallback(callback));
    }
  }
  
  private WriteCallback wrapCallback(WriteCallback callback) {
    return new WriteCallback() {
      @Override public void writeSuccess() {
        backlog.decrementAndGet();
        if (callback != null) callback.writeSuccess();
      }

      @Override public void writeFailed(Throwable x) {
        backlog.decrementAndGet();
        if (callback != null) callback.writeFailed(x);
      }
    };
  }
  
  private boolean isBelowHWM() {
    final JettyEndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
  
  public void flush() throws IOException {
    getRemote().flush();
  }
  
  public void sendPing() throws IOException {
    getRemote().sendPing(ByteBuffer.allocate(0));
  }

  @Override
  public void close() throws IOException {
    getSession().close();
  }
}
