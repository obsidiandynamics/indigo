package com.obsidiandynamics.indigo.messagebus.zmq;

import java.util.concurrent.*;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

final class ZmqSharedSocket extends Thread {
  private final String socketAddress;
  
  private final Context context;
  
  private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(1000);

  ZmqSharedSocket(String socketAddress) {
    super("ZmqSharedSocket");
    this.socketAddress = socketAddress;
    context = ZMQ.context(1);
    start();
  }
  
  void send(String payload) {
    try {
      queue.put(payload);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  @Override
  public void run() {
    final Socket socket = context.socket(ZMQ.PUB);
    socket.bind(socketAddress);
    socket.setHWM(0);
    
    while (! Thread.interrupted()) {
      final String payload;
      try {
        payload = queue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        continue;
      }
      
      socket.send(payload);
    }
    
    socket.setLinger(0);
    socket.close();
  }

  void close() {
    interrupt();
    context.term();
    try {
      join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
