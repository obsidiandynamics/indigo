package com.obsidiandynamics.indigo.ws;

import java.util.concurrent.atomic.*;

import org.eclipse.jetty.websocket.api.*;

final class Endpoint extends WebSocketAdapter {
  private final EndpointManager manager;
  
  private final AtomicLong backlog = new AtomicLong();

  public Endpoint(EndpointManager manager) {
    this.manager = manager;
  }
  
  public static Endpoint clientOf(EndpointConfig config, MessageListener listener) {
    return new EndpointManager(config, listener).createEndpoint();
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
    final EndpointConfig config = manager.getConfig();
    if (backlog.get() > config.highWaterMark) {
      return;
    }
    
    backlog.incrementAndGet();
    getRemote().sendString(payload,  new WriteCallback() {
      @Override public void writeSuccess() {
        backlog.decrementAndGet();
        if (callback != null) callback.writeSuccess();
      }

      @Override public void writeFailed(Throwable x) {
        backlog.decrementAndGet();
        if (callback != null) callback.writeFailed(x);
      }
    });
  }
}
