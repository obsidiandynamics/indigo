package com.obsidiandynamics.indigo.xbus.zmq;

import java.util.*;
import java.util.concurrent.*;

import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import com.obsidiandynamics.indigo.xbus.zmq.ZmqReceiveQueue.*;

import zmq.*;

final class ZmqSharedSocket extends Thread {
  private final ZmqBus bus;
  
  private final String socketAddress;
  
  private final Context context;
  
  private final BlockingQueue<String> sendQueue = new ArrayBlockingQueue<>(1000);
  
  private final List<ZmqReceiveQueue> receiveQueues = new CopyOnWriteArrayList<>();
  
  private volatile boolean running = true;

  ZmqSharedSocket(ZmqBus bus, String socketAddress) {
    super("ZmqSharedSocket");
    this.bus = bus;
    this.socketAddress = socketAddress;
    context = ZMQ.context(1);
    start();
  }
  
  void addReceiveQueue(ZmqReceiveQueue q) {
    receiveQueues.add(q);
  }
  
  void removeReceiveQueue(ZmqReceiveQueue q) {
    receiveQueues.remove(q);
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
        receive(socket);
      } else {
        break;
      }
    }
    
    socket.setLinger(0);
    socket.close();
    context.term();
  }
  
  private void receive(Socket socket) {
    final String str;
    try {
      str = socket.recvStr();
    } catch (ZMQException e) {
      if (e.getErrorCode() == ZError.ETERM) {
        socket.setLinger(0);
        socket.close();
        return;
      } else {
        e.printStackTrace();
        return;
      }
    }
    
    if (str != null) {
      final Received r = new Received(bus.getCodec().decode(str));
      for (ZmqReceiveQueue q : receiveQueues) {
        q.offer(r);
      }
    }
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
