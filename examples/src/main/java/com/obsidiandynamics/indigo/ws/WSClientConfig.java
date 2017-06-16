package com.obsidiandynamics.indigo.ws;

public class WSClientConfig {
  public int idleTimeoutMillis;
  public int scanIntervalMillis = 100;
  
  public boolean hasIdleTimeout() {
    return idleTimeoutMillis != 0;
  }
}
