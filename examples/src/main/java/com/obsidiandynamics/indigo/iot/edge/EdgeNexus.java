package com.obsidiandynamics.indigo.iot.edge;

import java.net.*;
import java.util.concurrent.*;
import java.util.function.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class EdgeNexus implements AutoCloseable {
  private final EdgeNode node;
  
  private final WSEndpoint endpoint;

  public EdgeNexus(EdgeNode node, WSEndpoint endpoint) {
    this.node = node;
    this.endpoint = endpoint;
  }
  
  public CompletableFuture<Void> sendAuto(Frame frame) {
    return SendHelper.sendAuto(frame, endpoint, node.getWire());
  }
  
  public void sendAuto(Frame frame, Consumer<Throwable> callback) {
    SendHelper.sendAuto(frame, endpoint, node.getWire(), callback);
  }
  
  public CompletableFuture<Void> send(TextEncodedFrame frame) {
    return SendHelper.send(frame, endpoint, node.getWire());
  }
  
  public void send(TextEncodedFrame frame, Consumer<Throwable> callback) {
    SendHelper.send(frame, endpoint, node.getWire(), callback);
  }
  
  public CompletableFuture<Void> send(BinaryEncodedFrame frame) {
    return SendHelper.send(frame, endpoint, node.getWire());
  }
  
  public void send(BinaryEncodedFrame frame, Consumer<Throwable> callback) {
    SendHelper.send(frame, endpoint, node.getWire(), callback);
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
