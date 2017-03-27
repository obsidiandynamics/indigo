package com.obsidiandynamics.indigo.benchmark;

import java.io.*;

abstract class LogConfig {
  boolean summary;
  boolean stages;
  boolean verbose;
  PrintStream out = System.out;
}
