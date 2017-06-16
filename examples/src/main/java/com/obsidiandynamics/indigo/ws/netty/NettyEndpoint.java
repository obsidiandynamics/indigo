package com.obsidiandynamics.indigo.ws.netty;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.ws.*;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.*;

public final class NettyEndpoint implements WSEndpoint {
  private final NettyEndpointManager manager;
  private final ChannelHandlerContext handlerContext;
  private final AtomicLong backlog = new AtomicLong();
  private final AtomicBoolean closeFired = new AtomicBoolean();
  
  private volatile Object context;

  NettyEndpoint(NettyEndpointManager manager, ChannelHandlerContext handlerContext) {
    this.manager = manager;
    this.handlerContext = handlerContext;
  }
  
  public ChannelHandlerContext getHandlerContext() {
    return handlerContext;
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
  public void send(ByteBuffer payload, SendCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      final ByteBuf buf = Unpooled.wrappedBuffer(payload);
      final ChannelFuture f = handlerContext.channel().writeAndFlush(new BinaryWebSocketFrame(buf));
      f.addListener(wrapCallback(callback));
    }
  }
  
  @Override
  public void send(String payload, SendCallback callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      final ChannelFuture f = handlerContext.channel().writeAndFlush(new TextWebSocketFrame(payload));
      f.addListener(wrapCallback(callback));
    }
  }
  
  private GenericFutureListener<ChannelFuture> wrapCallback(SendCallback callback) {
    return f -> {
      backlog.decrementAndGet();
      if (callback != null) {
        if (f.isSuccess()) {
          callback.onComplete(this);
        } else {
          callback.onError(this, f.cause());
        }
      }
    };
  }
  
  private boolean isBelowHWM() {
    final NettyEndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
  
  @Override
  public void sendPing() {
    handlerContext.channel().writeAndFlush(new PingWebSocketFrame());
  }

  @Override
  public boolean isOpen() {
    return handlerContext.channel().isOpen();
  }
  
  @Override
  public void flush() {
    handlerContext.channel().flush();
  }

  @Override
  public void terminate() throws IOException {
    if (handlerContext.channel().isOpen()) {
      handlerContext.channel().close();
    }
    fireCloseEvent();
  }

  @Override
  public void close() throws Exception {
    if (handlerContext.channel().isOpen()) {
      handlerContext.close().get();
    } else {
      fireCloseEvent();
    }
  }
  
  void fireCloseEvent() {
    if (closeFired.compareAndSet(false, true)) {
      manager.remove(handlerContext.channel());
      manager.getListener().onClose(this);
    }
  }
  
  void onPong() {
    //TODO
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) handlerContext.channel().remoteAddress();
  }

  @Override
  public long getBacklog() {
    return backlog.get();
  }

  @Override
  public long getLastActivityTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String toString() {
    return "NettyEndpoint [channel=" + handlerContext.channel() + "]";
  }
}
