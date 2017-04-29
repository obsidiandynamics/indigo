package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.io.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.util.*;

public class Diagnostics {
  public static final class Key {
    public static final String TRACE_ENABLED = "indigo.diagnostics.traceEnabled";
    public static final String LOG_SIZE = "indigo.diagnostics.logSize";
    private Key() {}
  }
  
  /** Whether tracing should be enabled. */
  public boolean traceEnabled = get(Key.TRACE_ENABLED, Boolean::parseBoolean, false);
  
  /** The upper bound on the size of the trace log. Beyond this, truncation from the head (least recent) occurs. */
  public int logSize = get(Key.LOG_SIZE, Integer::parseInt, 100_000);
  
  private final BlockingQueue<LogEntry> log = new LinkedBlockingQueue<>();
  
  final void init() {
    if (traceEnabled && ! Assertions.areEnabled()) {
      throw new IllegalStateException("Assertions need to be enabled for tracing, run JVM with -ea flag");
    }
  }
  
  public static final class LogEntry {
    private final String format;
    private final Object[] args;
    
    LogEntry(String format, Object ... args) {
      this.format = format;
      this.args = args;
    }
    
    @Override public String toString() {
      return String.format(format, args);
    }
  }

  public boolean traceMacro(String format, Object ... args) {
    trace(format, args);
    return true;
  }
  
  public void trace(String format, Object ... args) {
    if (traceEnabled) {
      log.add(new LogEntry(format, args));
      if (log.size() > logSize) {
        log.poll();
      }
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
