package com.obsidiandynamics.indigo.messagebus;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

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
    final String str = socket.recvStr();
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
  }
}
