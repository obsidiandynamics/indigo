package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.util.*;

public abstract class ServerHarness<E extends WSEndpoint> extends BaseHarness {
  public final AtomicLong connected = new AtomicLong();
  public final AtomicLong closed = new AtomicLong();
  
  public abstract List<E> getEndpoints();
  
  public abstract void broadcast(List<E> endpoints, byte[] payload);
  
  public abstract void flush(List<E> endpoints) throws IOException;
  
  public abstract void sendPing(E endpoint);
  
  protected final void keepAlive(E endpoint, AtomicBoolean ping, int idleTimeout) {
    if (idleTimeout != 0) Threads.asyncDaemon(() -> {
      while (ping.get()) {
        sendPing(endpoint);
        TestSupport.sleep(idleTimeout / 2);
      }
    }, "PingThread");
  }
}
