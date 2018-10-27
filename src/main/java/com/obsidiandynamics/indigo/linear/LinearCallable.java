package com.obsidiandynamics.indigo.linear;

import static com.obsidiandynamics.func.Functions.*;

import java.util.concurrent.*;

import com.obsidiandynamics.func.*;

/**
 *  Combines {@link LinearTask} with {@link Callable}, defining an ordered
 *  task that may return a value or throw an exception.
 */
public interface LinearCallable<V> extends Callable<V>, LinearTask {
  /**
   *  A helper for decorating an existing {@link Callable} instance
   *  with a {@link LinearTask}, on the basis of the provided key.
   *  
   *  @param <V> Return type.
   *  @param callable The callable to delegate to.
   *  @param key The key used for ordering.
   *  @return A {@link LinearCallable} instance.
   */
  public static <V> LinearCallable<V> decorate(Callable<V> callable, String key) {
    mustExist(callable, withMessage("Callable cannot be null", NullArgumentException::new));
    mustExist(key, withMessage("Key cannot be null", NullArgumentException::new));
    return new LinearCallable<V>() {
      @Override
      public V call() throws Exception {
        return callable.call();
      }

      @Override
      public String getKey() {
        return key;
      }
    };
  }
}
