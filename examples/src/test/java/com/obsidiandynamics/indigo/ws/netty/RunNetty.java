package com.obsidiandynamics.indigo.ws.netty;

public final class RunNetty {
  public static void main(String[] args) throws Exception {
    System.setProperty("io.netty.noUnsafe", Boolean.toString(true));
    final NettyServer netty = new NettyServer(6667, "/", null);
    netty.awaitTermination();
    netty.close();
  }
}
