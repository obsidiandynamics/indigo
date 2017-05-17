package com.obsidiandynamics.indigo.messagebus;

import java.util.function.*;

import com.obsidiandynamics.indigo.util.*;

public interface MessageSubscriber extends SafeCloseable {
  Object receive();
  
  default void onReceive(Consumer<Object> receiver) {
    Threads.asyncDaemon(() -> {
      for (;;) {
        final Object received = receive();
        if (received != null) {
          receiver.accept(received);
        } else {
          break;
        }
      }
    }, "Receiver");
  }
}
