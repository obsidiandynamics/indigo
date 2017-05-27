package com.obsidiandynamics.indigo.iot.client;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.ws.*;

public final class Session implements AutoCloseable {
  private final SessionManager manager;

  private final Map<UUID, CompletableFuture<SubscribeResponseFrame>> subscribeRequests = new ConcurrentHashMap<>();
  
  private WSEndpoint endpoint;
  
  Session(SessionManager manager) {
    this.manager = manager;
  }

  public WSEndpoint getEndpoint() {
    return endpoint;
  }

  void setEndpoint(WSEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  public InetSocketAddress getRemoteAddress() {
    return endpoint.getRemoteAddress();
  }
  
  CompletableFuture<SubscribeResponseFrame> removeSubscribeRequest(UUID id) {
    return subscribeRequests.remove(id);
  }
  
  public CompletableFuture<SubscribeResponseFrame> subscribe(SubscribeFrame sub) {
    final CompletableFuture<SubscribeResponseFrame> future = new CompletableFuture<>();
    subscribeRequests.put(sub.getId(), future);
    SendHelper.send(sub, endpoint, manager.getWire());
    return future;
  }
  
  public CompletableFuture<Void> publish(PublishFrame pub) {
    return SendHelper.send(pub, endpoint, manager.getWire());
  }
  
  @Override
  public void close() throws Exception {
    endpoint.close();
  }

  @Override
  public String toString() {
    return "Session [remote=" + getRemoteAddress() + "]";
  }
}
