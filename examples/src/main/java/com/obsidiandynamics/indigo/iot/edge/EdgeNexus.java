package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class EdgeNexus implements AutoCloseable {
  private final Edge edge;
  
  private final WSEndpoint endpoint;

  public EdgeNexus(Edge edge, WSEndpoint endpoint) {
    this.edge = edge;
    this.endpoint = endpoint;
  }
  
  public CompletableFuture<Void> send(TextEncodedFrame frame) {
    return SendHelper.send(frame, endpoint, edge.getWire());
  }
  
  public void send(TextEncodedFrame frame, Consumer<Throwable> callback) {
    SendHelper.send(frame, endpoint, edge.getWire(), callback);
  }
  
  public CompletableFuture<Void> send(BinaryEncodedFrame frame) {
    return SendHelper.send(frame, endpoint, edge.getWire());
  }
  
  public void send(BinaryEncodedFrame frame, Consumer<Throwable> callback) {
    SendHelper.send(frame, endpoint, edge.getWire(), callback);
  }
  
  public InetSocketAddress getPeerAddress() {
    return endpoint.getRemoteAddress();
  }

  @Override
  public void close() throws Exception {
    endpoint.close();
  }

  @Override
  public String toString() {
    return "EdgeNexus [peer=" + getPeerAddress() + "]";
  }
}
