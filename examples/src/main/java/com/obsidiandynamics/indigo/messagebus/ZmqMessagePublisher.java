package com.obsidiandynamics.indigo.messagebus;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

public final class ZmqMessagePublisher implements MessagePublisher {
  private final ZmqMessageBus bus;
  
  private final Context context;
  
  private final Socket socket;

  ZmqMessagePublisher(ZmqMessageBus bus) {
    this.bus = bus;
    context = ZMQ.context(1);
    socket = context.socket(ZMQ.PUB);
    socket.bind(bus.getSocketAddress());
  }
  
  @Override
  public void send(Object message) {
    socket.send(bus.getCodec().encode(message));
  }

  @Override
  public void close() {
    socket.close();
    context.term();
  }
}
