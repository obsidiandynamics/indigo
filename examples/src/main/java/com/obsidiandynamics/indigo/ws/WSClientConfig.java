package com.obsidiandynamics.indigo.ws;

public class WSClientConfig {
  public int idleTimeoutMillis = 300_000;
  
  public int scanIntervalMillis = 1_000;
  
  public WSEndpointConfig endpointConfig = new WSEndpointConfig();
  
  public boolean hasIdleTimeout() {
    return idleTimeoutMillis != 0;
  }
}
