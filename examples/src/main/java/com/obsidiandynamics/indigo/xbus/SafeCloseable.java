package com.obsidiandynamics.indigo.xbus;

public interface SafeCloseable extends AutoCloseable {
  @Override
  void close();
}
