package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import io.undertow.websockets.core.*;

public final class UndertowEndpoint extends AbstractReceiveListener {
  private final UndertowEndpointManager manager;
  
  private final WebSocketChannel channel;
  
  private final AtomicLong backlog = new AtomicLong();

  public UndertowEndpoint(UndertowEndpointManager manager, WebSocketChannel channel) {
    this.manager = manager;
    this.channel = channel;
  }
  
  public static UndertowEndpoint clientOf(WebSocketChannel channel, UndertowEndpointConfig config, UndertowMessageListener listener) {
    return new UndertowEndpointManager(config, listener).createEndpoint(channel);
  }
  
  @Override
  protected void onFullTextMessage(final WebSocketChannel channel, BufferedTextMessage message) throws IOException {
    manager.getMessageListener().onText(channel, message);
    super.onFullTextMessage(channel, message);
  }

  @Override
  protected void onFullBinaryMessage(final WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
    manager.getMessageListener().onBinary(channel, message);
    super.onFullBinaryMessage(channel, message);
  }

  @Override
  protected void onCloseMessage(CloseMessage message, WebSocketChannel channel) {
    manager.getMessageListener().onClose(channel, message.getCode(), message.getReason());
    super.onCloseMessage(message, channel);
  }
  
  @Override
  protected void onError(WebSocketChannel channel, Throwable cause) {
    manager.getMessageListener().onError(channel, cause);
    super.onError(channel, cause);
  }
  
  public void send(String payload, WebSocketCallback<Void> callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      WebSockets.sendText(payload, channel, wrapCallback(callback));
    }
  }
  
  public void send(ByteBuffer payload, WebSocketCallback<Void> callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      WebSockets.sendBinary(payload, channel, wrapCallback(callback));
    }
  }
  
  private WebSocketCallback<Void> wrapCallback(WebSocketCallback<Void> callback) {
    return new WebSocketCallback<Void>() {
      private final AtomicBoolean onceOnly = new AtomicBoolean();
      
      @Override public void complete(WebSocketChannel channel, Void context) {
        if (onceOnly.compareAndSet(false, true)) {
          backlog.decrementAndGet();
          if (callback != null) callback.complete(channel, context);
        }
      }

      @Override public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
        if (onceOnly.compareAndSet(false, true)) {
          backlog.decrementAndGet();
          if (callback != null) callback.onError(channel, context, throwable);
        }
      }
    };
  }
  
  private boolean isBelowHWM() {
    final UndertowEndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
  
  public void flush() {
    channel.flush();
  }
  
  public void close() throws IOException {
    channel.sendClose();
  }
}
