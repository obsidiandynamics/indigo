package com.obsidiandynamics.indigo.socketx.netty;

import com.obsidiandynamics.indigo.socketx.*;

public final class RunNetty {
  public static void main(String[] args) throws Exception {
    System.setProperty("io.netty.noUnsafe", Boolean.toString(true));
    final XServer<NettyEndpoint> netty = NettyServer.factory().create(new XServerConfig() {{
      port = 6667;
      contextPath = "/";
    }}, null);
    netty.close();
  }
}
