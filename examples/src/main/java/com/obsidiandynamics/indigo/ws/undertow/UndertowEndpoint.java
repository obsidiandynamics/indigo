package com.obsidiandynamics.indigo.ws.undertow;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.ws.*;

import io.undertow.websockets.core.*;

public final class UndertowEndpoint extends AbstractReceiveListener implements WSEndpoint {
  private final UndertowEndpointManager manager;
  
  private final WebSocketChannel channel;
  
  private final AtomicLong backlog = new AtomicLong();
  
  private Object context;

  UndertowEndpoint(UndertowEndpointManager manager, WebSocketChannel channel) {
    this.manager = manager;
    this.channel = channel;
  }
  
  public static UndertowEndpoint clientOf(WebSocketChannel channel, UndertowEndpointConfig config, EndpointListener<? super UndertowEndpoint> listener) {
    return new UndertowEndpointManager(config, listener).createEndpoint(channel);
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
  protected void onFullTextMessage(final WebSocketChannel channel, BufferedTextMessage message) throws IOException {
    manager.getListener().onText(this, message.getData());
    super.onFullTextMessage(channel, message);
  }

  @Override
  protected void onFullBinaryMessage(final WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
    manager.getListener().onBinary(this, WebSockets.mergeBuffers(message.getData().getResource()));
    super.onFullBinaryMessage(channel, message);
  }

  @Override
  protected void onCloseMessage(CloseMessage message, WebSocketChannel channel) {
    manager.getListener().onClose(this, message.getCode(), message.getReason());
    super.onCloseMessage(message, channel);
    if (channel.isCloseFrameSent() && channel.isOpen()) {
      try {
        channel.close();
      } catch (IOException e) {}
    }
  }
  
  @Override
  protected void onError(WebSocketChannel channel, Throwable cause) {
    manager.getListener().onError(this, cause);
    super.onError(channel, cause);
  }
  
  @Override
  public void send(String payload, SendCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      WebSockets.sendText(payload, channel, wrapCallback(callback));
    }
  }
  
  @Override
  public void send(ByteBuffer payload, SendCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      WebSockets.sendBinary(payload, channel, wrapCallback(callback));
    }
  }
  
  private WebSocketCallback<Void> wrapCallback(SendCallback callback) {
    return new WebSocketCallback<Void>() {
      private final AtomicBoolean onceOnly = new AtomicBoolean();
      
      @Override public void complete(WebSocketChannel channel, Void context) {
        if (onceOnly.compareAndSet(false, true)) {
          backlog.decrementAndGet();
          if (callback != null) callback.onComplete(UndertowEndpoint.this);
        }
      }

      @Override public void onError(WebSocketChannel channel, Void context, Throwable cause) {
        if (onceOnly.compareAndSet(false, true)) {
          backlog.decrementAndGet();
          if (callback != null) callback.onError(UndertowEndpoint.this, cause);
        }
      }
    };
  }
  
  private boolean isBelowHWM() {
    final UndertowEndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
  
  public WebSocketChannel getChannel() {
    return channel;
  }
  
  @Override
  public void flush() {
    channel.flush();
  }

  @Override
  public void sendPing() {
    if (channel.isOpen()) {
      WebSockets.sendPing(ByteBuffer.allocate(0), channel, null);
    }
  }

  @Override
  public void close() throws IOException {
    if (channel.isOpen()) {
      channel.sendClose();
    }
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return channel.getSourceAddress();
  }

  @Override
  public long getBacklog() {
    return backlog.get();
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public String toString() {
    return "UndertowEndpoint [channel=" + channel + "]";
  }
}
