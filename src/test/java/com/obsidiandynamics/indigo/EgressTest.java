package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static com.obsidiandynamics.indigo.TestSupport.*;
import static java.util.concurrent.TimeUnit.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.await;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class EgressTest implements TestSupport {
  private static final String DRIVER = "driver";
  private static final String DONE_RUNS = "done_runs";
  private static final String EXTERNAL = "external";
  
  private static final Executor EXECUTOR = TestSupport.oneTimeExecutor(EXTERNAL);

  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {}.createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test
  public void testFunction_serial() throws InterruptedException {
    testFunction(5, 10, false);
  }
  
  @Test
  public void testFunction_parallel() throws InterruptedException {
    testFunction(5, 10, true);
  }
  
  private void testFunction(int actors, int runs, boolean parallel) throws InterruptedException {
    final Set<ActorRef> doneRuns = new HashSet<>();
    
    system
    .useExecutor(EXECUTOR).named("custom")
    .on(DRIVER).cue(IntegerState::new, (a, m, s) -> {
      egressMode(a.<Integer, Integer>egress(in -> {
        assertEquals(EXTERNAL, Thread.currentThread().getName());
        return in + 1; 
      }), parallel)
      .withExecutor("custom")
      .ask(s.value).onResponse(r -> {
        assertFalse("Driven by an external thread", Thread.currentThread().getName().equals(EXTERNAL));
        
        final int res = r.body();
        if (res == runs) {
          a.to(ActorRef.of(DONE_RUNS)).tell();
        } else {
          assertEquals(s.value + 1, res);
          s.value = res;
          a.toSelf().tell();
        }
      });
    })
    .on(DONE_RUNS).cue(refCollector(doneRuns))
    .ingress(a -> {
      for (int i = 0; i < actors; i++) {
        a.to(ActorRef.of(DRIVER, i + "")).tell();
      }
    })
    .drain(0);

    assertEquals(actors, doneRuns.size());
  }
  
  @Test
  public void testConsumer_serial() throws InterruptedException {
    testConsumer(5, false);
  }
  
  @Test
  public void testConsumer_parallel() throws InterruptedException {
    testConsumer(5, true);
  }
  
  private void testConsumer(int runs, boolean parallel) throws InterruptedException {
    final Set<Integer> received = new CopyOnWriteArraySet<>();
    
    system
    .useExecutor(EXECUTOR).named("custom")
    .ingress().times(runs).act((a, i) -> {
      egressMode(a.<Integer>egress(in -> received.add(in)), parallel)
      .withExecutor("custom")
      .ask(i)
      .onResponse(r -> assertNull(r.body()));
    })
    .drain(0);
    
    assertEquals(runs, received.size());
  }
  
  @Test
  public void testRunnableAsk_serial() throws InterruptedException {
    testRunnableAsk(5, false);
  }
  
  @Test
  public void testRunnableAsk_parallel() throws InterruptedException {
    testRunnableAsk(5, true);
  }
  
  private void testRunnableAsk(int runs, boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();
    
    system
    .useExecutor(EXECUTOR).named("custom")
    .ingress().times(runs).act((a, i) -> {
      egressMode(a.egress(() -> { received.incrementAndGet(); }), parallel)
      .withExecutor("custom")
      .ask()
      .onResponse(r -> assertNull(r.body()));
    })
    .drain(0);
    
    assertEquals(runs, received.get());
  }
  
  @Test
  public void testRunnableTell_serial() throws InterruptedException {
    testRunnableTell(5, false);
  }

  @Test
  public void testRunnableTell_parallel() throws InterruptedException {
    testRunnableTell(5, true);
  }
  
  private void testRunnableTell(int runs, boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();
    
    system
    .useExecutor(EXECUTOR).named("custom")
    .ingress().times(runs).act((a, i) -> {
      egressMode(a.egress(() -> { received.incrementAndGet(); }), parallel)
      .withExecutor("custom")
      .tell();
    })
    .drain(0);
    
    if (parallel) await().atMost(10, SECONDS).until(() -> received.get() == runs);
    assertEquals(runs, received.get());
  }

  @Test
  public void testRunnableWithIllegalValue_serial() throws InterruptedException {
    testRunnableWithIllegalValue(false);
  }

  @Test
  public void testRunnableWithIllegalValue_parallel() throws InterruptedException {
    testRunnableWithIllegalValue(true);
  }
  
  private void testRunnableWithIllegalValue(boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();

    system.getConfig().exceptionHandler = DRAIN;
    system
    .useExecutor(EXECUTOR).named("custom")
    .ingress(a -> {
      egressMode(a.egress(() -> { received.incrementAndGet(); }), parallel)
      .withExecutor("custom")
      .ask("foo")
      .onFault(f -> assertIllegalArgumentException(f.getReason()))
      .onResponse(r -> assertNull(r.body()));
    });
    
    try {
      system.drain(0);
      fail("Failed to catch UnhandledMultiException");
    } catch (UnhandledMultiException e) {
      assertEquals(1, e.getErrors().length);
      assertIllegalArgumentException(e.getErrors()[0]);
    }
    
    assertEquals(0, received.get());
  }
  
  @Test
  public void testSupplier_serial() throws InterruptedException {
    testSupplier(5, false);
  }
  
  @Test
  public void testSupplier_parallel() throws InterruptedException {
    testSupplier(5, true);
  }
  
  private void testSupplier(int runs, boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();
    
    system
    .useExecutor(EXECUTOR).named("custom")
    .ingress().times(runs).act((a, i) -> {
      egressMode(a.egress(() -> received.incrementAndGet()), parallel)
      .withExecutor("custom")
      .ask()
      .onResponse(r -> assertEquals(Integer.class, r.body().getClass()));
    })
    .drain(0);
    
    assertEquals(runs, received.get());
  }
  
  @Test
  public void testAsyncSupplier_serial() throws InterruptedException {
    testAsyncSupplier(5, false);
  }
  
  @Test
  public void testAsyncSupplier_parallel() throws InterruptedException {
    testAsyncSupplier(5, true);
  }
  
  private void testAsyncSupplier(int runs, boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger processed = new AtomicInteger();
    
    system.ingress().times(runs).act((a, i) -> {
      egressMode(a.egressAsync(in -> {
        assertNull(in);
        final int newVal = received.incrementAndGet();
        final CompletableFuture<Integer> f = new CompletableFuture<>();
        Threads.asyncDaemon(() -> {
          processed.incrementAndGet();
          f.complete(newVal);
        }, "AsyncIngress");
        return f;
      }), parallel)
      .ask()
      .onResponse(r -> assertEquals(Integer.class, r.body().getClass()));
    })
    .drain(0);
    
    assertEquals(runs, received.get());
    assertEquals(runs, processed.get());
  }

  @Test
  public void testSupplierWithIllegalValue_serial() throws InterruptedException {
    testSupplierWithIllegalValue(false);
  }

  @Test
  public void testSupplierWithIllegalValue_parallel() throws InterruptedException {
    testSupplierWithIllegalValue(true);
  }

  private void testSupplierWithIllegalValue(boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();

    system.getConfig().exceptionHandler = DRAIN;
    system
    .useExecutor(EXECUTOR).named("custom")
    .ingress(a -> {
      egressMode(a.egress(() -> received.incrementAndGet()), parallel)
      .withExecutor("custom")
      .ask("foo")
      .onFault(f -> assertIllegalArgumentException(f.getReason()))
      .onResponse(r -> assertNull(r.body()));
    });
    
    try {
      system.drain(0);
      fail("Failed to catch UnhandledMultiException");
    } catch (UnhandledMultiException e) {
      assertEquals(1, e.getErrors().length);
      assertIllegalArgumentException(e.getErrors()[0]);
    }
    
    assertEquals(0, received.get());
  }
  
  @Test
  public void testAsyncSupplierWithIllegalValue_serial() throws InterruptedException {
    testAsyncSupplierWithIllegalValue(false);
  }
  
  @Test
  public void testAsyncSupplierWithIllegalValue_parallel() throws InterruptedException {
    testAsyncSupplierWithIllegalValue(true);
  }
  
  private void testAsyncSupplierWithIllegalValue(boolean parallel) throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();

    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      egressMode(a.egressAsync(() -> CompletableFuture.completedFuture(received.incrementAndGet())), parallel)
      .ask("foo")
      .onFault(f -> assertIllegalArgumentException(f.getReason()))
      .onResponse(r -> assertNull(r.body()));
    });
    
    try {
      system.drain(0);
      fail("Failed to catch UnhandledMultiException");
    } catch (UnhandledMultiException e) {
      assertEquals(1, e.getErrors().length);
      assertIllegalArgumentException(e.getErrors()[0]);
    }
    
    assertEquals(0, received.get());
  }
  
  @Test
  public void testExecutionOrder() throws InterruptedException {
    final int runs = 1000;
    final IntegerState s = new IntegerState();
    
    system.ingress(a -> {
      for (int i = 0; i < runs; i++) {
        a.egress(_i -> {
          assertEquals(s.value, _i);
          s.value++;
        })
        .withCommonPool()
        .tell(i);
      }
    })
    .drain(0);
    
    assertEquals(runs, s.value);
  }
  
  private void assertIllegalArgumentException(Throwable t) {
    assertEquals(IllegalArgumentException.class, t.getClass());
    assertEquals("Cannot pass a value to this egress lambda", t.getMessage());
  }
}