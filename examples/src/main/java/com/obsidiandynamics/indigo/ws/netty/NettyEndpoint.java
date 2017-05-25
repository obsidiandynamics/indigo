package com.obsidiandynamics.indigo.ws.netty;

import java.net.*;
import java.nio.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.ws.*;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.*;

public final class NettyEndpoint implements WSEndpoint<NettyEndpoint> {
  private final NettyEndpointManager manager;
  private final ChannelHandlerContext context;
  private final AtomicLong backlog = new AtomicLong();

  NettyEndpoint(NettyEndpointManager manager, ChannelHandlerContext context) {
    this.manager = manager;
    this.context = context;
  }
  
  public ChannelHandlerContext getContext() {
    return context;
  }
  
  @Override
  public void send(ByteBuffer payload, SendCallback<? super NettyEndpoint> callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      final ByteBuf buf = Unpooled.wrappedBuffer(payload);
      final ChannelFuture f = context.channel().writeAndFlush(new BinaryWebSocketFrame(buf));
      f.addListener(wrapCallback(callback));
    }
  }
  
  @Override
  public void send(String payload, SendCallback<? super NettyEndpoint> callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      final ChannelFuture f = context.channel().writeAndFlush(new TextWebSocketFrame(payload));
      f.addListener(wrapCallback(callback));
    }
  }
  
  private GenericFutureListener<ChannelFuture> wrapCallback(SendCallback<? super NettyEndpoint> callback) {
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
    context.channel().writeAndFlush(new PingWebSocketFrame());
  }
  
  @Override
  public void flush() {
    context.channel().flush();
  }

  @Override
  public void close() throws Exception {
    manager.remove(context);
    context.close().get();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) context.channel().remoteAddress();
  }
}
