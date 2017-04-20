package com.obsidiandynamics.indigo.util;

import java.util.concurrent.atomic.*;

public interface Integral64 {
  static final class Sum {
    private long value;
    private boolean certain;
    
    public long get() {
      return value;
    }
    
    void set(long value) {
      this.value = value;
    }
    
    public boolean isCertain() {
      return certain;
    }
    
    void setCertain(boolean certain) {
      this.certain = certain;
    }
    
    Sum certain(long value) {
      this.value = value;
      certain = true;
      return this;
    }
    
    Sum uncertain(long value) {
      this.value = value;
      certain = false;
      return this;
    }

    @Override
    public String toString() {
      return "Sum [value=" + value + ", certain=" + certain + "]";
    }
  }
  
  void add(long amount);
  
  Sum sum(Sum sum);
  
  default Sum sumCertain(Sum sum) {
    for (;;) {
      sum(sum);
      if (sum.certain) {
        return sum;
      } else {
        Thread.yield();
      }
    }
  }
  
  default long sumCertain() {
    return sumCertain(new Sum()).value;
  }
  
  static final class TripleStriped implements Integral64 {
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
    public Sum sum(Sum sum) {
      final long intentsBefore = intents.sum();
      final long completesBefore = completes.sum();
      sum.set(value.sum());
      if (intentsBefore == completesBefore) {
        final long intentsAfter = intents.sum();
        sum.setCertain(intentsBefore == intentsAfter);
      } else {
        sum.setCertain(false);
      }
      return sum;
    }
  }
  
  static final class Atomic implements Integral64 {
    private final AtomicLong value = new AtomicLong();

    @Override
    public void add(long amount) {
      value.addAndGet(amount);
    }

    @Override
    public Sum sum(Sum sum) {
      return sum.certain(value.get());
    }
    
    @Override
    public long sumCertain() {
      return value.get();
    }
  }
}