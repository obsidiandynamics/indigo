package com.obsidiandynamics.indigo.marketstrategy;

import com.obsidiandynamics.indigo.marketstrategy.Order.*;

public final class SimpleStochastic implements Strategy {
  private final StochasticOscillator oscillator; 
  
  private final float overboughtLevel;
  
  private final float oversoldLevel;
  
  public SimpleStochastic(int lookback, int kPeriod, float overboughtLevel, float oversoldLevel) {
    oscillator = new StochasticOscillator(lookback, kPeriod);
    this.overboughtLevel = overboughtLevel;
    this.oversoldLevel = oversoldLevel;
  }

  @Override
  public Order onBar(Bar bar) {
    final StochasticOutput out = oscillator.add(bar);
    if (out != null) {
      if (out.getD() <= oversoldLevel) {
        return new Order(bar.getSymbol(), Side.BUY, 100 - out.getD());
      } else if (out.getD() >= overboughtLevel) {
        return new Order(bar.getSymbol(), Side.SELL, out.getD());
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
