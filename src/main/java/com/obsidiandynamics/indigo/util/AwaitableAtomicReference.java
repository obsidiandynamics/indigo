package com.obsidiandynamics.indigo.util;

import java.util.concurrent.atomic.*;

public final class AwaitableAtomicReference<T> extends AtomicReference<T> {
  private static final long serialVersionUID = 1L;

  public T awaitThenGet() {
    for (;;) {
      final T val = get();
      if (val != null) {
        return val;
      } else {
        Thread.yield();
      }
    }
  }
}
