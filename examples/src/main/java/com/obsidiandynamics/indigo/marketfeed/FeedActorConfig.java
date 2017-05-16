package com.obsidiandynamics.indigo.marketfeed;

public class FeedActorConfig {
  /** The depth of market to aggregate the quotes for. */
  int depth;
  
  /** The tolerance around the clustering of quotes - the upper bound on the number of milliseconds
   *  that the quote timestamps may differ by. */
  int toleranceMillis;
}
