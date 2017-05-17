package com.obsidiandynamics.indigo.messagebus;

import java.util.*;
import java.util.concurrent.*;

public final class ZmqMessageBus implements MessageBus {
  private final String socketAddress;
  
  private final MessageCodec codec;
  
  private final Set<SafeCloseable> endpoints = new CopyOnWriteArraySet<>();

  public ZmqMessageBus(String socketAddress, MessageCodec codec) {
    this.socketAddress = socketAddress;
    this.codec = codec;
  }
  
  String getSocketAddress() {
    return socketAddress;
  }

  @Override
  public ZmqMessagePublisher getPublisher(String topic) {
    final ZmqMessagePublisher pub = new ZmqMessagePublisher(this, topic);
    endpoints.add(pub);
    return pub;
  }

  @Override
  public ZmqMessageSubscriber getSubscriber(String topic) {
    final ZmqMessageSubscriber sub = new ZmqMessageSubscriber(this, topic);
    endpoints.add(sub);
    return sub;
  }
  
  void remove(SafeCloseable endpoint) {
    endpoints.remove(endpoint);
  }

  MessageCodec getCodec() {
    return codec;
  }

  @Override
  public void close() {
    for (SafeCloseable endpoint : endpoints) {
      endpoint.close();
    }
  }
}
