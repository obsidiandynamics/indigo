package com.obsidiandynamics.indigo.linear;

import static com.obsidiandynamics.func.Functions.*;

import com.obsidiandynamics.func.*;

/**
 *  Combines {@link LinearTask} with {@link Runnable}, defining an ordered
 *  task that doesn't return a value or throw an exception.
 */
public interface LinearRunnable extends Runnable, LinearTask {
  /**
   *  A helper for decorating an existing {@link Runnable} instance
   *  with a {@link LinearTask}, on the basis of the provided key.
   *  
   *  @param runnable The runnable to delegate to.
   *  @param key The key used for ordering.
   *  @return A {@link LinearRunnable} instance.
   */
  public static LinearRunnable decorate(Runnable runnable, String key) {
    mustExist(runnable, "Runnable cannot be null");
    mustExist(key, "Key cannot be null");
    return new LinearRunnable() {
      @Override
      public void run() {
        runnable.run();
      }

      @Override
      public String getKey() {
        return key;
      }
    };
  }
}
