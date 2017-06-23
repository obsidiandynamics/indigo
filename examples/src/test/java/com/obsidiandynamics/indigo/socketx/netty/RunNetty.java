package com.obsidiandynamics.indigo.ws.netty;

import com.obsidiandynamics.indigo.ws.*;

public final class RunNetty {
  public static void main(String[] args) throws Exception {
    System.setProperty("io.netty.noUnsafe", Boolean.toString(true));
    final WSServer<NettyEndpoint> netty = NettyServer.factory().create(new WSServerConfig() {{
      port = 6667;
      contextPath = "/";
    }}, null);
    netty.close();
  }
}
