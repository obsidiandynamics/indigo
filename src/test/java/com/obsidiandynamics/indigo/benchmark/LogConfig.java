package com.obsidiandynamics.indigo.benchmark;

import java.io.*;

abstract class LogConfig {
  boolean enabled;
  boolean verbose;
  PrintStream out = System.out;
}
