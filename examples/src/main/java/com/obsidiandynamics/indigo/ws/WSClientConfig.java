package com.obsidiandynamics.indigo.ws;

public class WSClientConfig {
  public int idleTimeoutMillis;
  
  public boolean hasIdleTimeout() {
    return idleTimeoutMillis != 0;
  }
}
