package com.obsidiandynamics.indigo.marketfeed;

import java.util.*;

import com.obsidiandynamics.indigo.marketfeed.FeedContract.*;

final class FeedActorState {
  private final List<Quote> quotes = new ArrayList<>();
  
  MarketDepth addAndAggregate(Quote toAdd, FeedActorConfig config) {
    quotes.add(toAdd);
    final Optional<Quote> mostRecent = getMostRecentQuote();
    discardOldQuotes(mostRecent.get().getTimestamp() - config.toleranceMillis);
    return tryAggregate(config.depth);
  }
  
  private Optional<Quote> getMostRecentQuote() {
    return quotes.stream().max((q1, q2) -> Long.compare(q1.getTimestamp(), q2.getTimestamp()));
  }
  
  private MarketDepth tryAggregate(int depth) {
    if (quotes.size() >= depth) {
      final Quote[] bids = new Quote[depth];
      final Quote[] asks = new Quote[depth];
      
      for (Quote quote : quotes) {
        final Quote[] vector = quote.getType() == QuoteType.BID ? bids : asks;
        if (quote.getDepth() <= depth) {
          final Quote existing = vector[quote.getDepth() - 1];
          if (quote.getTimestamp() > existing.getTimestamp()) {
            vector[quote.getDepth() - 1] = quote;
          }
        }
      }
      
      if (isComplete(bids) && isComplete(asks)) {
        quotes.clear();
        return new MarketDepth(bids, asks);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
  
  private boolean isComplete(Quote[] quotes) {
    for (Quote quote : quotes) {
      if (quote == null) return false;
    }
    return true;
  }
  
  private void discardOldQuotes(long olderThan) {
    for (Iterator<Quote> it = quotes.iterator(); it.hasNext(); ) {
      final Quote quote = it.next();
      if (quote.getTimestamp() < olderThan) {
        it.remove();
      }
    }
  }
}
