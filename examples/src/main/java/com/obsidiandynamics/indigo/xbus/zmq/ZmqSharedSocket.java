package com.obsidiandynamics.indigo.xbus.zmq;

import java.util.concurrent.*;

import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import zmq.*;

final class ZmqSharedSocket extends Thread {
  private final String socketAddress;
  
  private final Context context;
  
  private final BlockingQueue<String> sendQueue = new ArrayBlockingQueue<>(1000);
  
  private volatile boolean running = true;

  ZmqSharedSocket(String socketAddress) {
    super("ZmqSharedSocket");
    this.socketAddress = socketAddress;
    context = ZMQ.context(1);
    start();
  }
  
  void send(String payload) {
    try {
      sendQueue.put(payload);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  @Override
  public void run() {
    final Socket socket = context.socket(ZMQ.PUB);
    socket.bind(socketAddress);
    socket.setHWM(0);
    
    for (;;) {    
      final String payload;
      try {
        payload = sendQueue.poll(10, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      
      if (running) {
        if (payload != null) {
          try {
            socket.send(payload);
          } catch (ZMQException e) {
            if (e.getErrorCode() == ZError.ETERM) {
              break;
            } else {
              e.printStackTrace();
            }
          }
        }
      } else {
        break;
      }
    }
    
    socket.setLinger(0);
    socket.close();
    context.term();
  }

  void close() {
    running = false;
    context.term();
    try {
      join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
