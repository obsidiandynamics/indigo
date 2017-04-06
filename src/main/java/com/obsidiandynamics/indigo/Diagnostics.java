package com.obsidiandynamics.indigo;

import java.io.*;
import java.util.concurrent.*;

public abstract class Diagnostics {
  public boolean traceEnabled;
  
  final void init() {
    if (traceEnabled) {
      boolean caught = false;
      try {
        assert false;
      } catch (AssertionError e) {
        caught = true;
      }
      if (! caught) {
        throw new AssertionError("Assertions need to be enabled for tracing, run JVM with -ea flag");
      }
    }
  }
  
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

  public boolean traceMacro(String format, Object ... args) {
    trace(format, args);
    return true;
  }
  
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
