package com.obsidiandynamics.indigo;

import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public final class ExecutorParams {
  public final int parallelism;
  public final JvmVersion version;
  
  public ExecutorParams(int parallelism, JvmVersion version) {
    this.parallelism = parallelism;
    this.version = version;
  }
}