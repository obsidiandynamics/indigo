package com.obsidiandynamics.indigo.ws;

public class WSServerConfig {
  public int port;
  
  public String contextPath;
  
  public int idleTimeoutMillis;
  
  public int pingIntervalMillis;
  
  public long highWaterMark = Long.MAX_VALUE;
}
