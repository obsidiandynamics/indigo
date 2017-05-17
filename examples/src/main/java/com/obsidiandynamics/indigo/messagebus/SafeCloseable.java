package com.obsidiandynamics.indigo.messagebus;

public interface SafeCloseable extends AutoCloseable {
  @Override
  void close();
}
