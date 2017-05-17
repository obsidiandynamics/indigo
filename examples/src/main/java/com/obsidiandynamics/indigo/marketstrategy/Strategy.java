package com.obsidiandynamics.indigo.marketstrategy;

import com.obsidiandynamics.indigo.marketstrategy.*;

public interface Strategy {
  Order onBar(Bar bar);
}
