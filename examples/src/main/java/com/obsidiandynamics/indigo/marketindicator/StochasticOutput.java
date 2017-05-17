package com.obsidiandynamics.indigo.marketindicator;

public final class StochasticOutput extends IndicatorValue {
  private final float k;
  
  private final float d;

  public StochasticOutput(String symbol, float k, float d) {
    super(symbol);
    this.k = k;
    this.d = d;
  }
  
  public float getK() {
    return k;
  }

  public float getD() {
    return d;
  }

  @Override
  public String toString() {
    return "StochasticOutput [symbol=" + getSymbol() + ", k=" + k + ", d=" + d + "]";
  }
}
