package com.obsidiandynamics.indigo.ws.netty;

import java.util.Locale;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
  private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
    // ping and pong frames already handled

    if (frame instanceof TextWebSocketFrame) {
      // Send the uppercase string back.
      String request = ((TextWebSocketFrame) frame).text();
      logger.info("{} received {}", ctx.channel(), request);
      ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase(Locale.US)));
    } else {
      String message = "unsupported frame type: " + frame.getClass().getName();
      throw new UnsupportedOperationException(message);
    }
  }
}