package com.obsidiandynamics.indigo.xbus.zmq;

import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import com.obsidiandynamics.indigo.xbus.*;

import zmq.*;

public final class ZmqSubscriber implements XSubscriber {
  private final OwnerThread owner = new OwnerThread();
  
  private final ZmqBus bus;
  
  private final String topic;
  
  private final Context context;
  
  private final Socket socket;

  ZmqSubscriber(ZmqBus bus, String topic) {
    this.bus = bus;
    this.topic = topic;
    context = ZMQ.context(1);
    socket = context.socket(ZMQ.SUB);
    socket.connect(bus.getSocketAddress());
    socket.setHWM(0);
    socket.subscribe(topic.getBytes(ZMQ.CHARSET));
  }
  
  @Override
  public Object receive() {
    owner.verifyCurrent();
    
    final String str;
    try {
      str = socket.recvStr();
    } catch (ZMQException e) {
      if (e.getErrorCode() == ZError.ETERM) {
        socket.setLinger(0);
        socket.close();
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
  public void send(Object message) {
    if (message == null) throw new NullPointerException("Message cannot be null");
    owner.verifyCurrent();
    
    final String encoded = bus.getCodec().encode(message);
    socket.send(encoded);
  }
  
  @Override
  public void close() {
    if (owner.isCurrent()) {
      socket.setLinger(0);
      socket.close();
    }
    context.term();
    bus.remove(this);
  }
}
