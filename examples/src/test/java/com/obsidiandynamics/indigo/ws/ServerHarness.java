package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public abstract class ServerHarness<E> extends BaseHarness {
  public final AtomicInteger connected = new AtomicInteger();
  public final AtomicInteger closed = new AtomicInteger();
  
  public abstract List<E> getEndpoints();
  
  public abstract void broadcast(List<E> endpoints, byte[] payload);
  
  public abstract void flush(List<E> endpoints) throws IOException;
  
  public abstract void sendPong(E endpoint) throws IOException;
  
  protected final void keepAlive(E endpoint, AtomicBoolean ping, int idleTimeout) {
    if (idleTimeout != 0) Threads.asyncDaemon(() -> {
      while (ping.get()) {
        try {
          sendPong(endpoint);
        } catch (IOException e) {
          e.printStackTrace();
        }
        TestSupport.sleep(idleTimeout / 2);
      }
    }, "PingThread");
  }
}
