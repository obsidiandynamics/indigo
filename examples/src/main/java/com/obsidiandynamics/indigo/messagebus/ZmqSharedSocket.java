package com.obsidiandynamics.indigo.messagebus;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

final class ZmqSharedSocket {
  private final Context context;
  
  private final Socket socket;

  ZmqSharedSocket(String socketAddress) {
    context = ZMQ.context(1);
    socket = context.socket(ZMQ.PUB);
    socket.bind(socketAddress);
  }
  
  synchronized void send(String topic, String payload) {
    socket.send(payload);
  }

  void close() {
    socket.close();
    context.term();
  }
}
