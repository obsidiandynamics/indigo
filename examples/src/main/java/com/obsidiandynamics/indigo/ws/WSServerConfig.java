package com.obsidiandynamics.indigo.ws;

public class WSServerConfig {
  public int port = 6667;
  
  public String contextPath = "/";
  
  public int idleTimeoutMillis = 300_000;
  
  public int pingIntervalMillis = 60_000;
  
  public int scanIntervalMillis = 1_000;
  
  public WSEndpointConfig endpointConfig = new WSEndpointConfig();
}
