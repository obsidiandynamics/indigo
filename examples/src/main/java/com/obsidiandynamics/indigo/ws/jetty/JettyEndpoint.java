package com.obsidiandynamics.indigo.ws.jetty;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.*;

import com.obsidiandynamics.indigo.ws.*;

public final class JettyEndpoint extends WebSocketAdapter implements WSEndpoint, WebSocketPingPongListener {
  private static final byte[] ZERO_ARRAY = new byte[0];
  
  private final JettyEndpointManager manager;
  
  private final AtomicLong backlog = new AtomicLong();
  
  private final AtomicBoolean closeFired = new AtomicBoolean();
  
  private volatile Object context;
  
  private volatile long lastActivityTime;

  JettyEndpoint(JettyEndpointManager manager) {
    this.manager = manager;
    touchLastActivityTime();
  }
  
  static JettyEndpoint clientOf(Scanner<JettyEndpoint> scanner, 
                                JettyEndpointConfig config, WSEndpointListener<? super JettyEndpoint> listener) {
    return new JettyEndpointManager(scanner, 0, config, listener).createEndpoint();
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getContext() {
    return (T) context;
  }

  @Override
  public void setContext(Object context) {
    this.context = context;
  }
  
  @Override 
  public void onWebSocketConnect(Session session) {
    super.onWebSocketConnect(session);
    manager.add(this);
    manager.getListener().onConnect(this);
    touchLastActivityTime();
  }

  @Override 
  public void onWebSocketText(String message) {
    super.onWebSocketText(message);
    manager.getListener().onText(this, message);
    touchLastActivityTime();
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int offset, int len) {
    super.onWebSocketBinary(payload, offset, len);
    final ByteBuffer message = ByteBuffer.wrap(payload, offset, len);
    manager.getListener().onBinary(this, message);
    touchLastActivityTime();
  }
  
  @Override 
  public void onWebSocketClose(int statusCode, String reason) {
    super.onWebSocketClose(statusCode, reason);
    manager.getListener().onDisconnect(this, statusCode, reason);
    fireCloseEvent();
    touchLastActivityTime();
  }
  
  @Override 
  public void onWebSocketError(Throwable cause) {
    super.onWebSocketError(cause);
    manager.getListener().onError(this, cause);
  }
  
  @Override
  public void send(String payload, SendCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      getRemote().sendString(payload, wrapCallback(callback));
      touchLastActivityTime();
    }
  }
  
  @Override
  public void send(ByteBuffer payload, SendCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      getRemote().sendBytes(payload, wrapCallback(callback));
      touchLastActivityTime();
    }
  }
  
  private WriteCallback wrapCallback(SendCallback callback) {
    return new WriteCallback() {
      @Override public void writeSuccess() {
        backlog.decrementAndGet();
        if (callback != null) callback.onComplete(JettyEndpoint.this);
      }

      @Override public void writeFailed(Throwable cause) {
        backlog.decrementAndGet();
        if (callback != null) callback.onError(JettyEndpoint.this, cause);
      }
    };
  }
  
  private boolean isBelowHWM() {
    final JettyEndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
  
  @Override
  public void flush() throws IOException {
    getRemote().flush();
  }
  
  public void sendPing() {
    if (isOpen()) {
      try {
        getRemote().sendPing(ByteBuffer.wrap(ZERO_ARRAY));
        touchLastActivityTime();
      } catch (IOException e) {
        throw new RuntimeException(e); // sendPing() is async; it shouldn't throw an IOException
      }
    }
  }

  @Override
  public boolean isOpen() {
    return isConnected();
  }

  @Override
  public void close() throws IOException {
    final Session session = getSession();
    if (session != null && session.isOpen()) {
      getSession().close();
    } else {
      fireCloseEvent();
    }
  }
  
  private void fireCloseEvent() {
    if (closeFired.compareAndSet(false, true)) {
      manager.remove(this);
      manager.getListener().onClose(this);
    }
  }

  @Override
  public void terminate() throws IOException {
    final Session session = getSession();
    if (session != null && session.isOpen()) {
      session.close();
    }
    fireCloseEvent();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return getRemote().getInetSocketAddress();
  }

  @Override
  public long getBacklog() {
    return backlog.get();
  }

  @Override
  public long getLastActivityTime() {
    return lastActivityTime;
  }
  
  private void touchLastActivityTime() {
    lastActivityTime = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return "JettyEndpoint [session=" + getSession() + ", lastActivity=" + getLastActivityTimeInstant() + "]";
  }

  @Override
  public void onWebSocketPing(ByteBuffer payload) {
    manager.getListener().onPing(payload);
    touchLastActivityTime();
  }

  @Override
  public void onWebSocketPong(ByteBuffer payload) {
    manager.getListener().onPong(payload);
    touchLastActivityTime();
  }
}
