package com.obsidiandynamics.indigo.ws;

import java.util.concurrent.atomic.*;

public abstract class BaseHarness implements AutoCloseable {
  public final AtomicInteger received = new AtomicInteger();
  public final AtomicInteger sent = new AtomicInteger();
}
