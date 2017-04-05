package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.concurrent.*;

public abstract class Diagnostics {
  public boolean traceEnabled;
  
  public final class LogEntry {
    private final String format;
    private final Object[] args;
    
    LogEntry(String format, Object[] args) {
      this.format = format;
      this.args = args;
    }
    
    @Override public String toString() {
      return String.format(format, args);
    }
  }
  
  private final BlockingQueue<LogEntry> log = new LinkedBlockingQueue<>();
  
  public void trace(String format, Object ... args) {
    if (traceEnabled) {
      log.add(new LogEntry(format, args));
    }
  }
  
  public LogEntry[] getLog() {
    return log.toArray(new LogEntry[log.size()]);
  }
  
  public void print(PrintStream out) {
    for (LogEntry entry : log) {
      out.println(entry);
    }
  }
}
