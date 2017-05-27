package com.obsidiandynamics.indigo.ws.netty;

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
  
  private Object context;

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
  public void flush() {
    handlerContext.channel().flush();
  }

  @Override
  public void close() throws Exception {
    manager.remove(handlerContext);
    handlerContext.close().get();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) handlerContext.channel().remoteAddress();
  }

  @Override
  public long getBacklog() {
    return backlog.get();
  }
}
