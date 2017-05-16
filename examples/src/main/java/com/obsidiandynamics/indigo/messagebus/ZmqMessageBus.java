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
  public MessagePublisher getPublisher(String topic) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MessageSubscriber getSubscriber(String topic) {
    // TODO Auto-generated method stub
    return null;
  }

  MessageCodec getCodec() {
    return codec;
  }
}
