package com.obsidiandynamics.indigo.messagebus;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

public final class ZmqMessagePublisher implements MessagePublisher {
  private final ZmqMessageBus bus;
  
  private final String topic;
  
  private final Context context;
  
  private final Socket socket;

  ZmqMessagePublisher(ZmqMessageBus bus, String topic) {
    this.bus = bus;
    this.topic = topic;
    context = ZMQ.context(1);
    socket = context.socket(ZMQ.PUB);
    socket.bind(bus.getSocketAddress());
  }
  
  @Override
  public void send(Object message) {
    if (message == null) throw new NullPointerException("Message cannot be null");
    
    final String encoded = bus.getCodec().encode(message);
    final String payload = topic + " " + encoded;
    socket.send(payload);
  }

  @Override
  public void close() {
    socket.close();
    context.term();
    bus.remove(this);
  }
}
