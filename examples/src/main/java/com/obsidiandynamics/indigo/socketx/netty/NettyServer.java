package com.obsidiandynamics.indigo.socketx.netty;

import com.obsidiandynamics.indigo.socketx.*;

import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.nio.*;
import io.netty.handler.logging.*;

public final class NettyServer implements XServer<NettyEndpoint> {
  private final NettyEndpointManager manager;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final XEndpointScanner<NettyEndpoint> scanner;
  
  private final Channel channel;
  
  private NettyServer(XServerConfig config, XEndpointListener<? super NettyEndpoint> listener) throws InterruptedException {
    scanner = new XEndpointScanner<>(config.scanIntervalMillis, config.pingIntervalMillis);
    manager = new NettyEndpointManager(scanner, config.endpointConfig, listener);
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();
    
    final ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new WebSocketServerInitializer(manager, config.contextPath, null, 
                                                 config.idleTimeoutMillis));

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
  
  public static XServerFactory<NettyEndpoint> factory() {
    return NettyServer::new;
  }
}