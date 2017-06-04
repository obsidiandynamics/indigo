package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.obsidiandynamics.indigo.util.*;

public abstract class ServerHarness extends BaseHarness {
  public final AtomicLong connected = new AtomicLong();
  public final AtomicLong closed = new AtomicLong();
  
  public abstract List<WSEndpoint> getEndpoints();
  
  public abstract void broadcast(List<WSEndpoint> endpoints, byte[] payload);
  
  public abstract void broadcast(List<WSEndpoint> endpoints, String payload);
  
  public abstract void flush(List<WSEndpoint> endpoints) throws IOException;
  
  public abstract void sendPing(WSEndpoint endpoint);
  
  protected final void keepAlive(WSEndpoint endpoint, AtomicBoolean ping, int idleTimeout) {
    if (idleTimeout != 0) Threads.asyncDaemon(() -> {
      while (ping.get()) {
        sendPing(endpoint);
        TestSupport.sleep(idleTimeout / 2);
      }
    }, "PingThread");
  }
}
