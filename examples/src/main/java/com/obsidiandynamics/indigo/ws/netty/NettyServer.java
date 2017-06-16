package com.obsidiandynamics.indigo.ws.netty;

import com.obsidiandynamics.indigo.ws.*;

import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.nio.*;
import io.netty.handler.logging.*;

public final class NettyServer implements WSServer<NettyEndpoint> {
  private final NettyEndpointManager manager;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final Scanner<NettyEndpoint> scanner;
  
  private final Channel channel;
  
  private NettyServer(WSServerConfig config, WSEndpointListener<? super NettyEndpoint> listener) throws InterruptedException {
    scanner = new Scanner<>(config.scanIntervalMillis, true);
    final NettyEndpointConfig endpointConfig = new NettyEndpointConfig() {{
      highWaterMark = config.highWaterMark;
    }};
    manager = new NettyEndpointManager(scanner, endpointConfig, listener);
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();
    
    final ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new WebSocketServerInitializer(manager, config.contextPath, null));

    channel = b.bind(config.port).sync().channel();
  }
  
  @Override
  public void close() throws Exception {
    scanner.close();
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
    channel.closeFuture().sync();
  }

  @Override
  public NettyEndpointManager getEndpointManager() {
    return manager;
  }
  
  public static WSServerFactory<NettyEndpoint> factory() {
    return NettyServer::new;
  }
}