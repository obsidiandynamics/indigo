package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.*;
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
  
  private final List<LogEntry> log = new CopyOnWriteArrayList<>();
  
  public void trace(String format, Object ... args) {
    if (traceEnabled) {
      log.add(new LogEntry(format, args));
    }
  }
  
  public List<LogEntry> getLog() {
    return Collections.unmodifiableList(log);
  }
  
  public void print(PrintStream out) {
    for (LogEntry entry : log) {
      out.println(entry);
    }
  }
}
