package com.obsidiandynamics.indigo.marketstrategy;

import java.util.function.*;

public interface OrderHandler extends Consumer<Order>, AutoCloseable {
  @Override
  default void close() {}
}
