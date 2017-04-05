package com.obsidiandynamics.indigo.experimental;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * @test
 * @bug 8078490
 * @summary Test submission and execution of task without joining
 */
public class SubmissionTest {
  public static void main(String[] args) throws Throwable {
    final ForkJoinPool e = new ForkJoinPool(1);
    final AtomicBoolean b = new AtomicBoolean();
    final Runnable setFalse = () -> b.set(false);
    for (int i = 0; i < 100000; i++) {
      b.set(true);
      e.execute(setFalse);
      long st = System.nanoTime();
      while (b.get()) {
        if (System.nanoTime() - st >= TimeUnit.SECONDS.toNanos(10)) {
          throw new RuntimeException(String.format("Submitted task failed to execute after %d submissions, pool=%s", i, e));
        }
      }
    }
  }
}