package com.obsidiandynamics.indigo.marketfeed;

import java.util.*;

/**
 *  The public message contract for the feed actor.
 */
public class FeedContract {
  public static final String ROLE = "feed";
  
  /** This class is just a container for others; it doesn't require instantiation. */
  private FeedContract() {}
  
  public enum QuoteType {
    BID, ASK
  }
  
  public static final class Quote {
    private final String symbol;
    
    private final long timestamp;
    
    private final QuoteType type;
    
    private final float price;
    
    private final int volume;
    
    private final int depth;

    public Quote(String symbol, long timestamp, QuoteType type, float price, int volume, int depth) {
      this.symbol = symbol;
      this.timestamp = timestamp;
      this.type = type;
      this.price = price;
      this.volume = volume;
      this.depth = depth;
    }
    
    final String getSymbol() {
      return symbol;
    }

    final long getTimestamp() {
      return timestamp;
    }

    final QuoteType getType() {
      return type;
    }

    final float getPrice() {
      return price;
    }

    final int getVolume() {
      return volume;
    }

    final int getDepth() {
      return depth;
    }

    @Override
    public String toString() {
      return "Quote [symbol=" + symbol + ", timestamp=" + timestamp + ", type=" + type + ", price=" + price
             + ", volume=" + volume + ", depth=" + depth + "]";
    }
  }
  
  public static final class MarketDepth {
    private final Quote[] bids;
    private final Quote[] asks;
    
    MarketDepth(Quote[] bids, Quote[] asks) {
      this.bids = bids;
      this.asks = asks;
    }

    final Quote[] getBids() {
      return bids;
    }

    final Quote[] getAsks() {
      return asks;
    }

    @Override
    public String toString() {
      return "MarketDepth [bids=" + Arrays.toString(bids) + ", asks=" + Arrays.toString(asks) + "]";
    }
  }
}
