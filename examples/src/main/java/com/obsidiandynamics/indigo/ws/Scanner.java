package com.obsidiandynamics.indigo.ws;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

public final class Scanner<E extends WSEndpoint> extends Thread implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(Scanner.class);
  
  private final int scanIntervalMillis;
  private final boolean pingEnabled;
  private final Set<E> endpoints = new CopyOnWriteArraySet<>();
  
  private volatile boolean running = true;
  
  public Scanner(int scanIntervalMillis, boolean pingEnabled) {
    super(String.format("Scanner[scanInterval=%dms,ping=%b]", scanIntervalMillis, pingEnabled));
    this.scanIntervalMillis = scanIntervalMillis;
    this.pingEnabled = pingEnabled;
    start();
  }
  
  @Override
  public void run() {
    while (running) {
      try {
        for (E endpoint : endpoints) {
          if (! endpoint.isOpen()) {
              endpoint.close();
          }
        }
      } catch (Exception e) {
        LOG.warn("Unexpected error", e);
      }
      
      try {
        Thread.sleep(scanIntervalMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        continue;
      }
    }
  }
  
  public void addEndpoint(E endpoint) {
    endpoints.add(endpoint);
  }
  
  public void removeEndpoint(E endpoint) {
    endpoints.remove(endpoint);
  }
  
  public Collection<E> getEndpoints() {
    return Collections.unmodifiableSet(endpoints);
  }
  
  @Override
  public void close() throws InterruptedException {
    running = false;
    interrupt();
    join();
  }
}
