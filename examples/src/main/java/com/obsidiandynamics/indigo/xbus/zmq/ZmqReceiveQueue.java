package com.obsidiandynamics.indigo.xbus.zmq;

import java.util.concurrent.*;

final class ZmqReceiveQueue {
  static final class Received {
    final Object message;
    Received(Object message) {
      this.message = message;
    }
  }
  
  private final BlockingQueue<Received> queue = new LinkedBlockingQueue<>();
  
  void offer(Received r) {
    queue.offer(r);
  }
  
  Received take() throws InterruptedException {
    return queue.take();
  }
}
