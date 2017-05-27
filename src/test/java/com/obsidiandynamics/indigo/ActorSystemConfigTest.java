package com.obsidiandynamics.indigo;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.util.JvmVersionProvider.*;

public final class ActorSystemConfigTest implements TestSupport {
  @Test
  public void testAutoExecutorFactorySafe() {
    final ExecutorParams params = new ExecutorParams(1, new JvmVersion(1, 8, 0, 65));
    final ExecutorService executor = ActorSystemConfig.ExecutorChoice.AUTO.apply(params);
    try {
      assertTrue("executor.class=" + executor.getClass().getName(), executor instanceof ForkJoinPool);
    } finally {
      executor.shutdown();
    }
  }
  
  @Test
  public void testAutoExecutorFactoryUnsafe() {
    final ExecutorParams params = new ExecutorParams(1, new JvmVersion(1, 8, 0, 64));
    final ExecutorService executor = ActorSystemConfig.ExecutorChoice.AUTO.apply(params);
    try {
      assertTrue("executor.class=" + executor.getClass().getName(), executor instanceof ThreadPoolExecutor);
    } finally {
      executor.shutdown();
    }
  }
}
