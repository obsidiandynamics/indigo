package com.obsidiandynamics.indigo.xbus.zmq;

import com.obsidiandynamics.indigo.xbus.*;
import com.obsidiandynamics.indigo.xbus.zmq.ZmqReceiveQueue.*;

public final class ZmqPublisher implements XPublisher {
  private final ZmqBus bus;
  
  private final String topic;
  
  private final ZmqSharedSocket sharedSocket;
  
  private final ZmqReceiveQueue receiveQueue = new ZmqReceiveQueue();
  
  ZmqPublisher(ZmqBus bus, String topic, ZmqSharedSocket sharedSocket) {
    this.bus = bus;
    this.topic = topic;
    this.sharedSocket = sharedSocket;
    sharedSocket.addReceiveQueue(receiveQueue);
  }
  
  @Override
  public void send(Object message) {
    if (message == null) throw new NullPointerException("Message cannot be null");
    final String encoded = bus.getCodec().encode(message);
    final String payload = topic + " " + encoded;
    sharedSocket.send(payload);
  }
  
  @Override
  public Object receive() {
    final Received r;
    try {
      r = receiveQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
    return r.message;
  }

  @Override
  public void close() {
    sharedSocket.removeReceiveQueue(receiveQueue);
    bus.remove(this);
  }
}
