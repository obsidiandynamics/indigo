/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.obsidiandynamics.indigo.ws.netty;

import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.socket.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.*;
import io.netty.handler.ssl.*;
import io.netty.handler.timeout.*;

final class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
  private final String contextPath;
  private final SslContext sslCtx;
  private final NettyEndpointManager manager;
  private final int idleTimeoutMillis;

  WebSocketServerInitializer(NettyEndpointManager manager, String contextPath, 
                             SslContext sslCtx, int idleTimeoutMillis) {
    this.manager = manager;
    this.contextPath = contextPath;
    this.sslCtx = sslCtx;
    this.idleTimeoutMillis = idleTimeoutMillis;
  }

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    final ChannelPipeline pipeline = ch.pipeline();
    if (sslCtx != null) {
      pipeline.addLast(sslCtx.newHandler(ch.alloc()));
    }
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new IdleStateHandler(0, 0, idleTimeoutMillis, TimeUnit.MILLISECONDS) {
      @Override protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        super.channelIdle(ctx, evt);
        final NettyEndpoint endpoint = manager.remove(ctx.channel());
        if (endpoint != null) {
          endpoint.terminate();
        }
      }
    });
    pipeline.addLast(new WebSocketServerCompressionHandler());
    pipeline.addLast(new WebSocketServerProtocolHandler(contextPath, null, true) {
      @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        manager.createEndpoint(ctx);
      }
      
      @Override
      protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        super.decode(ctx, frame, out);
        if (frame instanceof CloseWebSocketFrame) {
          final NettyEndpoint endpoint = manager.remove(ctx.channel());
          if (endpoint != null) {
            final CloseWebSocketFrame closeFrame = (CloseWebSocketFrame) frame;
            manager.getListener().onDisconnect(endpoint, closeFrame.statusCode(), closeFrame.reasonText());
            endpoint.fireCloseEvent();
          }
        } else if (frame instanceof TextWebSocketFrame) {
          final NettyEndpoint endpoint = manager.get(ctx.channel());
          if (endpoint != null) {
            final TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            manager.getListener().onText(endpoint, textFrame.text());
          }
        } else if (frame instanceof BinaryWebSocketFrame) {
          final NettyEndpoint endpoint = manager.get(ctx.channel());
          if (endpoint != null) {
            final BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
            final ByteBuf buf = binaryFrame.content();
            final ByteBuffer message = ByteBuffer.allocate(buf.readableBytes());
            buf.readBytes(message);
            manager.getListener().onBinary(endpoint, message);
          }
        } else if (frame instanceof PongWebSocketFrame) {
          final NettyEndpoint endpoint = manager.get(ctx.channel());
          if (endpoint != null) {
            endpoint.onPong();
          }
        }
      }
      
      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        final NettyEndpoint endpoint = manager.get(ctx.channel());
        if (endpoint != null) {
          manager.getListener().onError(endpoint, cause);
        }
      }
    });
  }
}