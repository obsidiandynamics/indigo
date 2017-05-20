package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
public interface ThrowingFactory<T> {
  T create() throws Exception;
}
