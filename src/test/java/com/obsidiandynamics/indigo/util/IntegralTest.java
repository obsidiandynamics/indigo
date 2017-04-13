package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public final class IntegralTest implements TestSupport {
  public static final class FaultyIntegral extends LongAdder implements LongIntegral {
    private static final long serialVersionUID = 1L;

    @Override
    public Sum sum(Sum sum) {
      return sum.certain(sum());
    }
  }
  
  private static final boolean BIG_TEST = false;
  
  @Test
  public void testTripleStriped() {
    final Supplier<LongIntegral> integralFactory = LongIntegral.TripleStriped::new;
    
    test(4, 1_000, 100, integralFactory.get());
    if (BIG_TEST) {
      test(4, 20_000_000, 1_000, integralFactory.get());
    }
  }
  
  private void test(int baseThreads, int rotations, long runs, LongIntegral adder) {
    logTestName();
    
    final AtomicInteger workers = new AtomicInteger(baseThreads);

    ParallelJob.nonBlocking(baseThreads, i -> {
      for (int rotation = 0; rotation < rotations; rotation++) {
        ParallelJob.blocking(baseThreads, j -> {
          for (long k = 0; k < runs; k++) {
            adder.add(1);
            adder.add(-1);
          }
        }).run();
      }
      
      workers.decrementAndGet();
    }).run();

    while (workers.get() != 0) {
      for (int i = 0; i < 1_000; i++) {
        final long sum = adder.sumCertain();
        if (LOG) log("sum is %d\n", sum);
        assertTrue("sum is " + sum, sum >= 0);
      }
    }
  }
}
