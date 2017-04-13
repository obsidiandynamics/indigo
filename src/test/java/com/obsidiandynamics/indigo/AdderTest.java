package com.obsidiandynamics.indigo;

import static junit.framework.TestCase.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class AdderTest implements TestSupport {
  public static final class UncertainSum {
    long value;
    boolean certain;
    
    public void certain(long value) {
      this.value = value;
      certain = true;
    }
    
    public void uncertain(long value) {
      this.value = value;
      certain = false;
    }
  }
  
  public interface UncertainAdder {
    void add(long amount);
    void sum(UncertainSum sum);
    default long sum() {
      for (UncertainSum sum = new UncertainSum();;) {
        sum(sum);
        if (sum.certain) {
          return sum.value;
        }
      }
    }
  }
  
  public static final class JucLongAdder extends LongAdder implements UncertainAdder {
    private static final long serialVersionUID = 1L;

    @Override
    public void sum(UncertainSum sum) {
      sum.certain(sum());
    }
  }
  
  public static final class AtomicLongAdder extends AtomicLong implements UncertainAdder {
    private static final long serialVersionUID = 1L;

    @Override
    public void add(long amount) {
      addAndGet(amount);
    }

    @Override
    public void sum(UncertainSum sum) {
      sum.certain(get());
    }
  }
  
  public static final class VersionedLongAdder implements UncertainAdder {
    private final LongAdder intents = new LongAdder();
    private final LongAdder completes = new LongAdder();
    private final LongAdder value = new LongAdder();
    
    @Override
    public void add(long amount) {
      intents.increment();
      value.add(amount);
      completes.increment();
    }

    @Override
    public void sum(UncertainSum sum) {
      final long intentsBefore = intents.sum();
      final long completesBefore = completes.sum();
      sum.value = value.sum();
      if (intentsBefore == completesBefore) {
        final long completesAfter = completes.sum();
        sum.certain = completesBefore == completesAfter;
      } else {
        sum.certain = false;
      }
    }
  }
  
  private static final boolean BIG_TEST = false;
  
  @Test
  public void test() {
    test(4, 1_000, 1_000, new VersionedLongAdder());
    if (BIG_TEST) {
      test(4, 100_000, 1_000, new VersionedLongAdder());
    }
  }
  
  private void test(int baseThreads, int rotations, long runs, UncertainAdder adder) {
    logTestName();
    
    final AtomicInteger workers = new AtomicInteger(baseThreads);

    final CyclicBarrier barrier = new CyclicBarrier(2);
    ParallelJob.nonBlocking(baseThreads, i -> {
      if (i == 0) {
        adder.add(1);
        Threads.await(barrier);
      }
      
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

    Threads.await(barrier);
    while (workers.get() != 0) {
      for (int i = 0; i < 1000; i++) {
        final long sum = adder.sum();
        if (LOG) log("sum is %d\n", sum);
        assertTrue("sum is " + sum, sum > 0);
      }
    }
  }
}
