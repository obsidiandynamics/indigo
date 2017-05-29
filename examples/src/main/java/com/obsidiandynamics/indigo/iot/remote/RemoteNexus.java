package com.obsidiandynamics.indigo.iot.remote;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RemoteNexus implements AutoCloseable {
  private final RemoteNode node;

  private final Map<UUID, CompletableFuture<SubscribeResponseFrame>> subscribeRequests = new ConcurrentHashMap<>();
  
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
  
  CompletableFuture<SubscribeResponseFrame> removeSubscribeRequest(UUID id) {
    return subscribeRequests.remove(id);
  }
  
  public CompletableFuture<SubscribeResponseFrame> subscribe(SubscribeFrame sub) {
    final CompletableFuture<SubscribeResponseFrame> future = new CompletableFuture<>();
    subscribeRequests.put(sub.getId(), future);
    SendHelper.send(sub, endpoint, node.getWire());
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

  @Override
  public String toString() {
    return "RemoteNexus [peer=" + getPeerAddress() + "]";
  }
}
