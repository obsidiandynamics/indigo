package com.obsidiandynamics.indigo.ws;

public class WSConfig {
  public int port;
  
  public String contextPath;
  
  public int idleTimeoutMillis;
  
  public int pingIntervalMillis;
  
  public long highWaterMark = Long.MAX_VALUE;
}
