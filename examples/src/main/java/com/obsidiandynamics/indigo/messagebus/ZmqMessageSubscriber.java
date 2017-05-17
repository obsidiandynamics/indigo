package com.obsidiandynamics.indigo.messagebus;

import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import zmq.*;

public final class ZmqMessageSubscriber implements MessageSubscriber {
  private final ZmqMessageBus bus;
  
  private final String topic;
  
  private final Context context;
  
  private final Socket socket;

  ZmqMessageSubscriber(ZmqMessageBus bus, String topic) {
    this.bus = bus;
    this.topic = topic;
    context = ZMQ.context(1);
    socket = context.socket(ZMQ.SUB);
    socket.connect(bus.getSocketAddress());
    socket.subscribe(topic.getBytes(ZMQ.CHARSET));
  }
  
  @Override
  public Object receive() {
    final String str;
    try {
      str = socket.recvStr();
    } catch (ZMQException e) {
      if (e.getErrorCode() == ZError.ETERM) {
        return null;
      } else {
        throw e;
      }
    }
    
    if (str != null) {
      final String encoded = str.substring(topic.length() + 1);
      return bus.getCodec().decode(encoded);
    } else {
      return null;
    }
  }

  @Override
  public void close() {
    socket.close();
    context.term();
    bus.remove(this);
  }
}
