package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class EdgeNexus implements AutoCloseable {
  private final Edge edge;
  
  private final WSEndpoint<?> endpoint;

  public EdgeNexus(Edge edge, WSEndpoint<?> endpoint) {
    this.edge = edge;
    this.endpoint = endpoint;
  }
  
  public void send(Frame frame, Consumer<Throwable> callback) {
    SendHelper.send(frame, endpoint, edge.getWire(), callback);
  }
  
  public CompletableFuture<Void> send(Frame frame) {
    return SendHelper.send(frame, endpoint, edge.getWire());
  }
  
  public InetSocketAddress getRemoteAddress() {
    return endpoint.getRemoteAddress();
  }

  @Override
  public void close() throws Exception {
    endpoint.close();
  }

  @Override
  public String toString() {
    return "EdgeNexus [remote=" + getRemoteAddress() + "]";
  }
}
