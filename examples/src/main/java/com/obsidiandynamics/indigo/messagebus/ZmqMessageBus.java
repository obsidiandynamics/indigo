package com.obsidiandynamics.indigo.messagebus;

public final class ZmqMessageBus implements MessageBus {
  private final String socketAddress;
  
  private final MessageCodec codec;

  public ZmqMessageBus(String socketAddress, MessageCodec codec) {
    this.socketAddress = socketAddress;
    this.codec = codec;
  }
  
  String getSocketAddress() {
    return socketAddress;
  }

  @Override
  public ZmqMessagePublisher getPublisher(String topic) {
    return new ZmqMessagePublisher(this, topic);
  }

  @Override
  public ZmqMessageSubscriber getSubscriber(String topic) {
    return new ZmqMessageSubscriber(this, topic);
  }

  MessageCodec getCodec() {
    return codec;
  }
}
