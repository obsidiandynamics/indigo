package com.obsidiandynamics.indigo.marketstrategy;

public final class Bar {
  private final String symbol;
  
  private final float open;
  
  private final float high;
  
  private final float low;
  
  private final float close;

  public Bar(String symbol, float open, float high, float low, float close) {
    this.symbol = symbol;
    this.open = open;
    this.high = high;
    this.low = low;
    this.close = close;
  }

  String getSymbol() {
    return symbol;
  }

  float getOpen() {
    return open;
  }

  float getHigh() {
    return high;
  }

  float getLow() {
    return low;
  }

  float getClose() {
    return close;
  }

  @Override
  public String toString() {
    return "Bar [symbol=" + symbol + ", open=" + open + ", high=" + high + ", low=" + low + ", close=" + close + "]";
  }
}
