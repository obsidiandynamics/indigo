package com.obsidiandynamics.indigo.ws.netty;

import java.nio.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.ws.*;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.*;

public final class NettyEndpoint implements WSEndpoint {
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
  
  public void send(ByteBuffer payload, GenericFutureListener<ChannelFuture> callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      final ByteBuf buf = Unpooled.wrappedBuffer(payload);
      final ChannelFuture f = context.channel().writeAndFlush(new BinaryWebSocketFrame(buf));
      f.addListener(wrapCallback(callback));
    }
  }
  
  public void send(String payload, GenericFutureListener<ChannelFuture> callback) {
    if (isBelowHWM()) {
      backlog.incrementAndGet();
      final ChannelFuture f = context.channel().writeAndFlush(new TextWebSocketFrame(payload));
      f.addListener(wrapCallback(callback));
    }
  }
  
  private GenericFutureListener<ChannelFuture> wrapCallback(GenericFutureListener<ChannelFuture> callback) {
    return f -> {
      backlog.decrementAndGet();
      if (callback != null) callback.operationComplete(f);
    };
  }
  
  private boolean isBelowHWM() {
    final NettyEndpointConfig config = manager.getConfig();
    return backlog.get() < config.highWaterMark;
  }
  
  public void sendPing() {
    context.channel().writeAndFlush(new PingWebSocketFrame());
  }
  
  public void flush() {
    context.channel().flush();
  }

  @Override
  public void close() throws Exception {
    manager.remove(context);
    context.close().get();
  }
}
