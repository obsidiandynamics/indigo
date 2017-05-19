package com.obsidiandynamics.indigo.marketstrategy;

public interface Strategy {
  Order onBar(Bar bar);
}
