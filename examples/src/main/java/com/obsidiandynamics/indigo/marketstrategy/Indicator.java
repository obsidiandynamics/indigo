package com.obsidiandynamics.indigo.marketstrategy;

public interface Indicator {
  IndicatorValue add(Bar bar);
}
