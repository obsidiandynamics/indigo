package com.obsidiandynamics.indigo.marketstrategy;

public final class Order {
  public enum Side {
    BUY, SELL
  }
  
  private final String symbol;
  
  private final Side side;
  
  private final float lots;

  public Order(String symbol, Side side, float lots) {
    this.symbol = symbol;
    this.side = side;
    this.lots = lots;
  }

  public String getSymbol() {
    return symbol;
  }

  public Side getSide() {
    return side;
  }

  public float getLots() {
    return lots;
  }

  @Override
  public String toString() {
    return "Order [symbol=" + symbol + ", side=" + side + ", lots=" + lots + "]";
  }
}
