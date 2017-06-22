package com.obsidiandynamics.indigo.util;

@FunctionalInterface
public interface ThrowingConsumer<T> {
  void accept(T t) throws Exception;
}
