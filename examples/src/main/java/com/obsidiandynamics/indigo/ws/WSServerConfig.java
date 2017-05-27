package com.obsidiandynamics.indigo.ws;

public class WSServerConfig {
  public int port = 6667;
  
  public String contextPath = "/";
  
  public int idleTimeoutMillis;
  
  public int pingIntervalMillis;
  
  public long highWaterMark = Long.MAX_VALUE;
}
