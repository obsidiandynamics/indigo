package com.obsidiandynamics.indigo.marketindicator;

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

  final String getSymbol() {
    return symbol;
  }

  final float getOpen() {
    return open;
  }

  final float getHigh() {
    return high;
  }

  final float getLow() {
    return low;
  }

  final float getClose() {
    return close;
  }

  @Override
  public String toString() {
    return "Bar [symbol=" + symbol + ", open=" + open + ", high=" + high + ", low=" + low + ", close=" + close + "]";
  }
}
