package com.obsidiandynamics.indigo.ws;

import java.nio.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.*;

final class Endpoint extends WebSocketAdapter {
  private final EndpointManager manager;
  
  private final AtomicLong backlog = new AtomicLong();

  public Endpoint(EndpointManager manager) {
    this.manager = manager;
  }
  
  public static Endpoint clientOf(EndpointConfig config, MessageListener listener) {
    return new EndpointManager(0, config, listener).createEndpoint();
  }
  
  @Override 
  public void onWebSocketConnect(Session session) {
    super.onWebSocketConnect(session);
    manager.getMessageListener().onConnect(session);
  }

  @Override 
  public void onWebSocketText(String message) {
    super.onWebSocketText(message);
    manager.getMessageListener().onText(getSession(), message);
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int offset, int len) {
    super.onWebSocketBinary(payload, offset, len);
    manager.getMessageListener().onBinary(getSession(), payload, offset, len);
  }
  
  @Override 
  public void onWebSocketClose(int statusCode, String reason) {
    super.onWebSocketClose(statusCode, reason);
    manager.getMessageListener().onClose(getSession(), statusCode, reason);
  }
  
  @Override 
  public void onWebSocketError(Throwable cause) {
    super.onWebSocketError(cause);
    manager.getMessageListener().onError(getSession(), cause);
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
    final EndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
}
