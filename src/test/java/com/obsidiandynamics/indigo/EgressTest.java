package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

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
  public void testFunction() throws InterruptedException {
    final int actors = 5;
    final int runs = 10;
    final Set<ActorRef> doneRuns = new HashSet<>();
    
    system.on(DRIVER).cue(IntegerState::new, (a, m, s) -> {
      a.<Integer, Integer>egress(in -> {
        assertEquals(EXTERNAL, Thread.currentThread().getName());
        return in + 1; 
      })
      .withExecutor(EXECUTOR)
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
  public void testConsumer() throws InterruptedException {
    final int runs = 5;
    final Set<Integer> received = new CopyOnWriteArraySet<>();
    
    system.ingress().times(runs).act((a, i) -> {
      a.<Integer>egress(in -> received.add(in))
      .withExecutor(EXECUTOR)
      .ask(i)
      .onResponse(r -> assertNull(r.body()));
    })
    .drain(0);
    
    assertEquals(runs, received.size());
  }
  
  @Test
  public void testRunnableAsk() throws InterruptedException {
    final int runs = 5;
    final AtomicInteger received = new AtomicInteger();
    
    system.ingress().times(runs).act((a, i) -> {
      a.egress(() -> { received.incrementAndGet(); })
      .withExecutor(EXECUTOR)
      .ask()
      .onResponse(r -> assertNull(r.body()));
    })
    .drain(0);
    
    assertEquals(runs, received.get());
  }
  
  @Test
  public void testRunnableTell() throws InterruptedException {
    final int runs = 5;
    final AtomicInteger received = new AtomicInteger();
    
    system.ingress().times(runs).act((a, i) -> {
      a.egress(() -> { received.incrementAndGet(); })
      .withExecutor(EXECUTOR)
      .tell();
    })
    .drain(0);
    
    await().atMost(10, TimeUnit.SECONDS).until(() -> received.get() == runs);
    assertEquals(runs, received.get());
  }
  
  @Test
  public void testRunnableWithIllegalValue() throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();

    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      a.egress(() -> { received.incrementAndGet(); })
      .withExecutor(EXECUTOR)
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
  public void testSupplier() throws InterruptedException {
    final int runs = 5;
    final AtomicInteger received = new AtomicInteger();
    
    system.ingress().times(runs).act((a, i) -> {
      a.egress(() -> received.incrementAndGet())
      .withExecutor(EXECUTOR)
      .ask()
      .onResponse(r -> assertEquals(Integer.class, r.body().getClass()));
    })
    .drain(0);
    
    assertEquals(runs, received.get());
  }
  
  @Test
  public void testAsyncSupplier() throws InterruptedException {
    final int runs = 5;
    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger processed = new AtomicInteger();
    
    system.ingress().times(runs).act((a, i) -> {
      a.egressAsync(in -> {
        assertNull(in);
        final int newVal = received.incrementAndGet();
        final CompletableFuture<Integer> f = new CompletableFuture<>();
        TestSupport.asyncDaemon(() -> {
          processed.incrementAndGet();
          f.complete(newVal);
        }, "AsyncIngress");
        return f;
      })
      .withCommonPool()
      .ask()
      .onResponse(r -> assertEquals(Integer.class, r.body().getClass()));
    })
    .drain(0);
    
    assertEquals(runs, received.get());
    assertEquals(runs, processed.get());
  }
  
  @Test
  public void testSupplierWithIllegalValue() throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();

    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      a.egress(() -> received.incrementAndGet())
      .withExecutor(EXECUTOR)
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
  public void testAsyncSupplierWithIllegalValue() throws InterruptedException {
    final AtomicInteger received = new AtomicInteger();

    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      a.egressAsync(() -> CompletableFuture.completedFuture(received.incrementAndGet()))
      .withCommonPool()
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
  
  private void assertIllegalArgumentException(Throwable t) {
    assertEquals(IllegalArgumentException.class, t.getClass());
    assertEquals("Cannot pass a value to this egress lambda", t.getMessage());
  }
}