package com.obsidiandynamics.indigo.marketstrategy;

import java.util.*;
import java.util.stream.*;

public final class StochasticOscillator implements Indicator {
  private final int lookback; 
  private final int kPeriod; 
  
  private final List<Bar> bars = new ArrayList<>();
  
  private final List<Float> kValues = new ArrayList<>();
  
  public StochasticOscillator(int lookback, int kPeriod) {
    this.lookback = lookback;
    this.kPeriod = kPeriod;
  }
  
  private static <T> void add(List<T> list, T value, int cap) {
    list.add(value);
    while (list.size() > cap) list.remove(0);
  }

  @Override
  public StochasticOutput add(Bar bar) {
    add(bars, bar, lookback);
    if (bars.size() == lookback) {
      final float lowestLow = bars.stream().min((b1, b2) -> Float.compare(b1.getLow(), b2.getLow())).get().getLow();
      final float highestHigh = bars.stream().max((b1, b2) -> Float.compare(b1.getHigh(), b2.getHigh())).get().getHigh();
      final float k = highestHigh != lowestLow ? (bar.getClose() - lowestLow) / (highestHigh - lowestLow) * 100 : 50;
      add(kValues, k, kPeriod);
      
      if (kValues.size() == kPeriod) {
        final float d = kValues.stream().collect(Collectors.averagingDouble(kValue -> kValue)).floatValue();
        return new StochasticOutput(bar.getSymbol(), k, d);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
