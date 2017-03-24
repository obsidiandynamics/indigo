package com.obsidiandynamics.indigo;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
  /**
   *  Performs this operation on the given arguments.
   *
   *  @param t The first input argument.
   *  @param u The second input argument.
   *  @param v The third input argument.
   */
  void accept(T t, U u, V v);
}
