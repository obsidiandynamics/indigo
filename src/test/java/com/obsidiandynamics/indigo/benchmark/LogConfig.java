package com.obsidiandynamics.indigo.benchmark;

import java.io.*;

public abstract class LogConfig {
  public boolean summary;
  public boolean stages;
  public boolean verbose;
  public PrintStream out = System.out;
}
