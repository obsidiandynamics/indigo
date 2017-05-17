package com.obsidiandynamics.indigo.marketstrategy;

public abstract class IndicatorValue {
  private final String symbol;

  public IndicatorValue(String symbol) {
    this.symbol = symbol;
  }
  
  public final String getSymbol() {
    return symbol;
  }
}
