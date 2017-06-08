package com.obsidiandynamics.indigo.iot.remote;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RemoteNexus implements AutoCloseable {
  private final RemoteNode node;

  private final Map<UUID, CompletableFuture<BindResponseFrame>> bindRequests = new ConcurrentHashMap<>();
  
  private WSEndpoint endpoint;
  
  RemoteNexus(RemoteNode node) {
    this.node = node;
  }

  public WSEndpoint getEndpoint() {
    return endpoint;
  }

  void setEndpoint(WSEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  public InetSocketAddress getPeerAddress() {
    return endpoint.getRemoteAddress();
  }
  
  CompletableFuture<BindResponseFrame> removeBindRequest(UUID id) {
    return bindRequests.remove(id);
  }
  
  public CompletableFuture<BindResponseFrame> bind(BindFrame bind) {
    final CompletableFuture<BindResponseFrame> future = new CompletableFuture<>();
    bindRequests.put(bind.getMessageId(), future);
    SendHelper.send(bind, endpoint, node.getWire());
    return future;
  }
  
  public CompletableFuture<Void> publish(PublishTextFrame pub) {
    return SendHelper.send(pub, endpoint, node.getWire());
  }
  
  public CompletableFuture<Void> publish(PublishBinaryFrame pub) {
    return SendHelper.send(pub, endpoint, node.getWire());
  }
  
  @Override
  public void close() throws Exception {
    endpoint.close();
  }
  
  public boolean awaitClose(int waitMillis) throws InterruptedException {
    return endpoint.awaitClose(waitMillis);
  }

  @Override
  public String toString() {
    return "RemoteNexus [peer=" + getPeerAddress() + "]";
  }
}
