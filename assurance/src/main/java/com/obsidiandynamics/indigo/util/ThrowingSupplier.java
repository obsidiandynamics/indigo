package com.obsidiandynamics.indigo.util;

@FunctionalInterface
public interface ThrowingSupplier<T> {
  T create() throws Exception;
}
