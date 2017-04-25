package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;
import static com.obsidiandynamics.indigo.util.Integral64.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public final class Integral64Test implements TestSupport {
  static final class FaultyIntegral extends LongAdder implements Integral64 {
    private static final long serialVersionUID = 1L;

    @Override
    public Sum sum(Sum sum) {
      return sum.certain(sum());
    }
  }
  
  private static final boolean BIG_TEST = false;
  
  @Test
  public void testTripleStriped() {
    final Supplier<Integral64> integralFactory = TripleStriped::new;
    
    test(4, 1_000, 100, integralFactory.get());
    if (BIG_TEST) {
      test(4, 1_000_000, 1_000, integralFactory.get());
    }
  }
  
  @Test
  public void testAtomic() {
    final Supplier<Integral64> integralFactory = Atomic::new;
    
    test(4, 100, 100, integralFactory.get());
  }
  
  private void test(int baseThreads, int rotations, long runs, Integral64 integral) {
    final AtomicInteger workers = new AtomicInteger(baseThreads);

    ParallelJob.nonBlocking(baseThreads, i -> {
      for (int rotation = 0; rotation < rotations; rotation++) {
        ParallelJob.blocking(baseThreads, j -> {
          for (long k = 0; k < runs; k++) {
            integral.add(1);
            integral.add(-1);
          }
        }).run();
      }
      
      workers.decrementAndGet();
    }).run();

    final Sum s = new Sum();
    while (workers.get() != 0) {
      for (int i = 0; i < 1_000; i++) {
        final long sum = i % 2 == 0 ? integral.sumCertain() : integral.sumCertain(s).get();
        if (LOG) log("sum is %d\n", sum);
        assertTrue("sum is " + sum, sum >= 0);
      }
    }
  }
}
