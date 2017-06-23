package com.obsidiandynamics.indigo.socketx;

import java.util.concurrent.atomic.*;

public abstract class BaseHarness implements AutoCloseable {
  public final AtomicLong received = new AtomicLong();
  public final AtomicLong sent = new AtomicLong();
}
