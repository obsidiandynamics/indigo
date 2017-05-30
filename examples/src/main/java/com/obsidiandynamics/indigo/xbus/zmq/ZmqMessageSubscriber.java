package com.obsidiandynamics.indigo.xbus.zmq;

import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import com.obsidiandynamics.indigo.xbus.*;

import zmq.*;

public final class ZmqMessageSubscriber implements MessageSubscriber {
  private final Thread owner;
  
  private final ZmqMessageBus bus;
  
  private final String topic;
  
  private final Context context;
  
  private final Socket socket;

  ZmqMessageSubscriber(ZmqMessageBus bus, String topic) {
    owner = Thread.currentThread();
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
    if (! isOwner()) throw new IllegalStateException("Can only be invoked by owner " + owner);
    
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
  
  private boolean isOwner() {
    return owner == Thread.currentThread();
  }

  @Override
  public void close() {
    if (isOwner()) {
      socket.setLinger(0);
      socket.close();
    }
    context.term();
    bus.remove(this);
  }
}
