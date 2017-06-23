package com.obsidiandynamics.indigo.socketx;

public class XClientConfig {
  public int idleTimeoutMillis = 300_000;
  
  public int scanIntervalMillis = 1_000;
  
  public XEndpointConfig endpointConfig = new XEndpointConfig();
  
  public boolean hasIdleTimeout() {
    return idleTimeoutMillis != 0;
  }
}
