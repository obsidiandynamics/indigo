package com.obsidiandynamics.indigo.util;

import static junit.framework.TestCase.*;

import java.util.concurrent.atomic.*;

import org.junit.*;

public class AwaitableAtomicReferenceTest {
  @Test
  public void testSet() throws InterruptedException {
    final AwaitableAtomicReference<Object> ref = new AwaitableAtomicReference<>();
    
    final AtomicBoolean got = new AtomicBoolean();
    
    final Thread setter = new Thread(() -> {
      ref.set(1);
    }, "setter");
    
    final Thread getter = new Thread(() -> {
      final Object obj = ref.awaitThenGet();
      assertEquals(1, obj);
      got.set(true);
    }, "getter");
    
    getter.start();
    setter.start();
    getter.join();
    
    assertTrue(got.get());
  }
}
