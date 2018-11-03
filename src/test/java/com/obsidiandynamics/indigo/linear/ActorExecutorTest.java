package com.obsidiandynamics.indigo.linear;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.*;

import com.obsidiandynamics.await.*;
import com.obsidiandynamics.func.*;
import com.obsidiandynamics.junit.*;
import com.obsidiandynamics.threads.*;

@RunWith(Parameterized.class)
public final class ActorExecutorTest {
  @Parameterized.Parameters
  public static List<Object[]> data() {
    return TestCycle.timesQuietly(1);
  }
  
  @Rule
  public final ExpectedException expectedException = ExpectedException.none();
  
  /** Multiplier for the number of runs used by the tests (if applicable). */
  private static final int RUNS_SCALE = 1;

  /** Multiplier for the number of keys used by the tests (if applicable). */
  private static final int KEYS_SCALE = 1;
  
  /** Number of threads to use for multithreaded tests. */
  private static final int PARALLELISM = 16;
  
  private static final Timesert await = Timesert.wait(10_000);
  
  private ActorExecutor executor;
  
  @After
  public void after() {
    if (executor != null) {
      try {
        executor.shutdownNow();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
  
  private void assertExecutorCleanedUp() throws InterruptedException {
    assertTrue(executor.isShutdown());
    assertTrue(executor.isTerminated());
    await.untilTrue(executor.getActorSystem()::isShuttingDown);

    assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    assertEquals(0, executor.getPendingTasks());
  }
  
  @Test
  public void testCreateAndShutdownNowWithNoTasks() throws InterruptedException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    assertFalse(executor.isShutdown());
    assertFalse(executor.isTerminated());
    assertFalse(executor.getActorSystem().isShuttingDown());
    
    assertEquals(Collections.emptyList(), executor.shutdownNow());
    assertExecutorCleanedUp();
  }
  
  @Test
  public void testCreateAndShutdownWithNoTasks() throws InterruptedException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    assertFalse(executor.isShutdown());
    assertFalse(executor.isTerminated());
    assertFalse(executor.getActorSystem().isShuttingDown());
    
    executor.shutdown();
    assertExecutorCleanedUp();
  }
  
  /**
   *  Verifies that null keys are unsupported.
   */
  @Test
  public void testExecuteWithNullKey() {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    
    expectedException.expect(NullArgumentException.class);
    expectedException.expectMessage("Key cannot be null");
    executor.submit(new LinearRunnable() {
      @Override
      public String getKey() { return null; }
      
      @Override
      public void run() {}
    });
  }
  
  /**
   *  Tests that non-linear tasks (which are allowed by default) can be rejected
   *  with the appropriate setting.
   */
  @Test
  public void testRejectNonLinearTasks() {
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(1)
                                  .withAllowNonLinearTasks(false));
    
    expectedException.expect(RejectedExecutionException.class);
    expectedException.expectMessage("Non-linear tasks are not allowed");
    try {
      executor.submit(() -> {});
    } finally {
      assertEquals(0, executor.getPendingTasks());
    }
  }
  
  /**
   *  Submits {@link Runnable} tasks for unordered execution, shuts down the executor and awaits for termination. 
   *  Tasks should all be complete by the time {@link ActorExecutor#awaitTermination(long, TimeUnit)}
   *  returns. <p>
   *  
   *  This test uses {@link ActorExecutor#submit(Runnable, Object)} with a nominated return value.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitRunnableRandomOrder() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = PARALLELISM;
    final int keys = 100 * KEYS_SCALE;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(parallelism));
    final int expectedTasks = runs * keys;
    final List<Future<Integer>> tasks = new ArrayList<>(expectedTasks);
    for (int run = 0; run < runs; run++) {
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        tasks.add(executor.submit(() -> {
          ints.addAndGet(_key, 1);
          Thread.yield();
        }, _key));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(expectedTasks, sumInts(ints));
    assertAllSucceeded(Classes.cast(tasks));
    assertExecutorCleanedUp();
    
    // the sum of all future task returns should equal the sum of an arithmetic progression
    assertEquals((keys - 1) * keys / 2 * runs, sumTaskResults(tasks));
  }
  
  /**
   *  Submits {@link Callable} tasks for unordered execution, shuts down the executor and awaits for termination. 
   *  Tasks should all be complete by the time {@link ActorExecutor#awaitTermination(long, TimeUnit)}
   *  returns. <p>
   *  
   *  This test uses {@link ActorExecutor#submit(Callable)}.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitCallableRandomOrder() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = PARALLELISM;
    final int keys = 100 * KEYS_SCALE;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(parallelism));
    final int expectedTasks = runs * keys;
    final List<Future<Integer>> tasks = new ArrayList<>(expectedTasks);
    for (int run = 0; run < runs; run++) {
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        tasks.add(executor.submit(() -> {
          ints.addAndGet(_key, 1);
          Thread.yield();
          return _key;
        }));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(expectedTasks, sumInts(ints));
    assertAllSucceeded(Classes.cast(tasks));
    assertExecutorCleanedUp();
    
    // the sum of all future task returns should equal the sum of an arithmetic progression
    assertEquals((keys - 1) * keys / 2 * runs, sumTaskResults(tasks));
  }
  
  /**
   *  Executes {@link Runnable} tasks for unordered execution, shuts down the executor and 
   *  awaits for termination. Tasks should all be complete by the time 
   *  {@link ActorExecutor#awaitTermination(long, TimeUnit)} returns. <p>
   *  
   *  This test uses {@link ActorExecutor#execute(Runnable)}.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testExecuteRunnableRandomOrder() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = PARALLELISM;
    final int keys = 100 * KEYS_SCALE;
    
    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(parallelism)
                                  .withAgentListener(agentCounter));
    for (int run = 0; run < runs; run++) {
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        executor.execute(() -> {
          ints.addAndGet(_key, 1);
          Thread.yield();
        });
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    final int expectedTasks = runs * keys;
    assertEquals(expectedTasks, sumInts(ints));
    assertExecutorCleanedUp();

    assertTrue("counts=" + agentCounter.getCounts(), agentCounter.getCounts().size() >= 1);
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }

  /**
   *  Submits {@link LinearRunnable} tasks for deterministic execution, shuts down the executor and 
   *  awaits for termination. Tasks should all be complete by the time 
   *  {@link ActorExecutor#awaitTermination(long, TimeUnit)} returns and the counters for each task 
   *  should be assigned sequentially with no discontinuities. <p>
   *  
   *  This test uses {@link ActorExecutor#submit(Runnable)}.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitLinearRunnableDeterministicOrder() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = PARALLELISM;
    final int keys = 10 * KEYS_SCALE;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(parallelism)
                                  .withAgentListener(agentCounter));
    final int expectedTasks = runs * keys;
    final List<Future<?>> tasks = new ArrayList<>(expectedTasks);
    for (int run = 1; run <= runs; run++) {
      final int _run = run;
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        tasks.add(executor.submit(LinearRunnable.decorate(() -> {
          randomSleep(0, 1);
          final int previousValue = ints.getAndSet(_key, _run);
          assertEquals("for key " + _key, _run - 1, previousValue);
        }, String.valueOf(_key))));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertAllSucceeded(tasks);
    assertEquals(expectedTasks, sumInts(ints));
    assertExecutorCleanedUp();
    
    assertTrue("counts=" + agentCounter.getCounts(), agentCounter.getCounts().size() >= 1);
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }  

  /**
   *  Submits {@link HashedRunnable} tasks for deterministic execution, shuts down the executor and 
   *  awaits for termination. This is equivalent of using a {@link LinearRunnable} with an equivalent key.
   *  Tasks should all be complete by the time {@link ActorExecutor#awaitTermination(long, TimeUnit)}
   *  returns and the counters for each task should be assigned sequentially with no discontinuities. <p>
   *  
   *  This test uses {@link ActorExecutor#submit(Runnable)}.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitHashedRunnableDeterministicOrder() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = PARALLELISM;
    final int keys = 10 * KEYS_SCALE;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(parallelism)
                                  .withAgentListener(agentCounter));
    final int expectedTasks = runs * keys;
    final List<Future<?>> tasks = new ArrayList<>(expectedTasks);
    for (int run = 1; run <= runs; run++) {
      final int _run = run;
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        tasks.add(executor.submit(LinearRunnable.decorate(() -> {
          randomSleep(0, 1);
          final int previousValue = ints.getAndSet(_key, _run);
          assertEquals("for key " + _key, _run - 1, previousValue);
        }, String.valueOf(_key))));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertAllSucceeded(tasks);
    assertEquals(expectedTasks, sumInts(ints));
    assertExecutorCleanedUp();
    
    assertTrue("counts=" + agentCounter.getCounts(), agentCounter.getCounts().size() >= 1);
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }
  
  /**
   *  Submits {@link LinearCallable} tasks for deterministic execution, shuts down the executor and 
   *  awaits for termination. Tasks should all be complete by the time 
   *  {@link ActorExecutor#awaitTermination(long, TimeUnit)} returns and the counters for each task 
   *  should be assigned sequentially with no discontinuities. <p>
   *  
   *  This test uses {@link ActorExecutor#submit(Callable)} and also adds bias to the agent actors.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitLinearCallableDeterministicOrderAndBias() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = 8;
    final int keys = 10 * KEYS_SCALE;
    final int actorBias = 10;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(parallelism)
                                  .withAgentListener(agentCounter)
                                  .withActorBias(actorBias));
    final int expectedTasks = runs * keys;
    final List<Future<Integer>> tasks = new ArrayList<>(expectedTasks);
    for (int run = 1; run <= runs; run++) {
      final int _run = run;
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        tasks.add(executor.submit(LinearCallable.decorate(() -> {
          final int previousValue = ints.getAndSet(_key, _run);
          assertEquals("for key " + _key, _run - 1, previousValue);
          return _key;
        }, String.valueOf(_key))));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(expectedTasks, sumInts(ints));
    assertAllSucceeded(Classes.cast(tasks));
    assertExecutorCleanedUp();
    
    // the sum of all future task returns should equal the sum of an arithmetic progression
    assertEquals((keys - 1) * keys / 2 * runs, sumTaskResults(tasks));
    
    assertEquals(keys, agentCounter.getCounts().size());
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }

  /**
   *  A variant of {@link #testSubmitLinearCallableDeterministicOrderAndBiasWithRandomTimings()} that
   *  introduces a random delay in the execution of each task.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitLinearCallableDeterministicOrderAndBiasWithRandomTimings() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = 8;
    final int keys = 10 * KEYS_SCALE;
    final int actorBias = 10;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(parallelism)
                                  .withAgentListener(agentCounter)
                                  .withActorBias(actorBias));
    final int expectedTasks = runs * keys;
    final List<Future<Integer>> tasks = new ArrayList<>(expectedTasks);
    for (int run = 1; run <= runs; run++) {
      final int _run = run;
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        tasks.add(executor.submit(LinearCallable.decorate(() -> {
          randomSleep(0, 1);
          final int previousValue = ints.getAndSet(_key, _run);
          assertEquals("for key " + _key, _run - 1, previousValue);
          return _key;
        }, String.valueOf(_key))));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(expectedTasks, sumInts(ints));
    assertAllSucceeded(Classes.cast(tasks));
    assertExecutorCleanedUp();
    
    // the sum of all future task returns should equal the sum of an arithmetic progression
    assertEquals((keys - 1) * keys / 2 * runs, sumTaskResults(tasks));
    
    assertEquals(keys, agentCounter.getCounts().size());
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }

  /**
   *  Executes {@link LinearRunnable} tasks deterministically, shuts down the executor and 
   *  awaits for termination. Tasks should all be complete by the time 
   *  {@link ActorExecutor#awaitTermination(long, TimeUnit)} returns and the counters for each task 
   *  should be assigned sequentially with no discontinuities. <p>
   *  
   *  This test uses {@link ActorExecutor#execute(Runnable)}.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testExecuteLinearRunnableDeterministicOrder() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = PARALLELISM;
    final int keys = 10 * KEYS_SCALE;

    final AtomicIntegerArray ints = new AtomicIntegerArray(keys);
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(parallelism)
                                  .withAgentListener(agentCounter));
    for (int run = 1; run <= runs; run++) {
      final int _run = run;
      for (int key = 0; key < keys; key++) {
        final int _key = key;
        executor.execute(LinearRunnable.decorate(() -> {
          randomSleep(0, 1);
          final int previousValue = ints.getAndSet(_key, _run);
          assertEquals("for key " + _key, _run - 1, previousValue);
        }, String.valueOf(_key)));
      }
    }
    
    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    final int expectedTasks = runs * keys;
    assertEquals(expectedTasks, sumInts(ints));
    assertExecutorCleanedUp();
    
    assertEquals(keys, agentCounter.getCounts().size());
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }  
  
  /**
   *  Submits tasks after the executor has been shut down, and verifies that the tasks have been
   *  cancelled.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testSubmitAfterShutdown() throws InterruptedException {
    final int runs = 10 * RUNS_SCALE;
    final int parallelism = 86;
    
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(parallelism));
    final AtomicInteger executionCount = new AtomicInteger();
    final ArrayList<Future<?>> tasks = new ArrayList<>(runs);
    
    assertEquals(Collections.emptyList(), executor.shutdownNow());
    for (int run = 0; run < runs; run++) {
      tasks.add(executor.submit(() -> {
        executionCount.incrementAndGet();
      }));
    }

    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertAllCancelled(tasks);
    assertExecutorCleanedUp();
  }
  
  /**
   *  Awaits termination on an executor that is still running (not shut down) in the
   *  main thread and times out while waiting.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testAwaitTerminationWhileRunningWithTimeout() throws InterruptedException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    assertFalse(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
    assertEquals(Collections.emptyList(), executor.shutdownNow());
    assertExecutorCleanedUp();
  }
  
  /**
   *  Awaits termination on an executor that is still running (not shut down) in the main
   *  thread with a single task that terminates the executor shortly after launching. 
   *  The main thread will eventually await the termination condition.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testAwaitTerminationWhileRunningWithSuccess() throws InterruptedException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    executor.execute(() -> {
      assertEquals(1, executor.getPendingTasks());
      Threads.sleep(10);
      executor.shutdown();
    });
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(Collections.emptyList(), executor.shutdownNow());
    assertExecutorCleanedUp();
  }
  
  /**
   *  Invokes {@link ActorExecutor#shutdownNow()} with pending tasks using the default 
   *  behaviour (to interrupt running tasks) and verifies that they have been cancelled.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testShutdownNowCancelPendingTasksWithInterrupt() throws InterruptedException {
    final int parallelism = PARALLELISM;
    final int numTasks = 10;
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(parallelism));
    
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    final List<LinearFutureTask<?>> tasks = new ArrayList<>(numTasks);
    for (int i = 0; i < numTasks; i++) {
      tasks.add(executor.submit(LinearRunnable.decorate(() -> {
        Threads.await(startBarrier);
        Threads.await(endBarrier);         // should be interrupted when the task is cancelled
        assertTrue(Thread.interrupted());
      }, "key")));
    }
    
    // wait for the first task to commence
    Threads.await(startBarrier);
    
    assertEquals(numTasks, executor.getPendingTasks());
    final List<Runnable> pendingTasks = executor.shutdownNow();
    assertEquals(numTasks, pendingTasks.size());
    
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertTrue(tasks.get(0).isDone());
    assertTrue(tasks.get(0).isCancelled());
    assertTrue(tasks.get(0).isCompletedExceptionally());
    assertFalse(tasks.get(0).isCompletedSuccessfully());
    for (int i = 1; i < numTasks; i++) {
      assertTrue(tasks.get(i).isDone());
      assertTrue(tasks.get(i).isCancelled());
      assertTrue(tasks.get(i).isCompletedExceptionally());
      assertFalse(tasks.get(i).isCompletedSuccessfully());
    }
    assertExecutorCleanedUp();
  }
  
  /**
   *  Invokes {@link ActorExecutor#shutdownNow(boolean)} with pending tasks without interrupting
   *  them and verifies that they have been cancelled.
   *  
   *  @throws InterruptedException
   */
  @Test
  public void testShutdownNowCancelPendingTasksWithoutInterrupt() throws InterruptedException {
    final int parallelism = PARALLELISM;
    final int numTasks = 10;
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(parallelism));
    
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    final ArrayList<LinearFutureTask<?>> tasks = new ArrayList<LinearFutureTask<?>>(numTasks);
    for (int i = 0; i < numTasks; i++) {
      tasks.add(executor.submit(LinearRunnable.decorate(() -> {
        Threads.await(startBarrier);
        Threads.await(endBarrier);
        assertFalse(Thread.interrupted());
      }, "key")));
    }
    
    // wait for the first task to commence
    Threads.await(startBarrier);
    
    assertEquals(numTasks, executor.getPendingTasks());
    final List<LinearFutureTask<?>> pendingTasks = executor.shutdownNow(false);
    assertEquals(numTasks, pendingTasks.size());
    
    // wait for the task to complete
    Threads.await(endBarrier);
    
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertTrue(tasks.get(0).isDone());
    assertTrue(tasks.get(0).isCancelled());
    assertTrue(tasks.get(0).isCompletedExceptionally());
    assertFalse(tasks.get(0).isCompletedSuccessfully());
    for (int i = 1; i < numTasks; i++) {
      assertTrue(tasks.get(i).isDone());
      assertTrue(tasks.get(i).isCancelled());
      assertTrue(tasks.get(i).isCompletedExceptionally());
      assertFalse(tasks.get(i).isCompletedSuccessfully());
    }
    assertExecutorCleanedUp();
  }
  
  /**
   *  Awaits for pending tasks after executor shutdown, where the task is blocked,
   *  resulting in a timeout.
   *  
   *  @throws InterruptedException
   *  @throws TimeoutException 
   */
  @Test
  public void testShutdownAndAwaitPendingTasksWithTimeout() throws InterruptedException, TimeoutException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    final LinearFutureTask<?> task = executor.submit(LinearRunnable.decorate(() -> {
      Threads.await(startBarrier);
      Threads.await(endBarrier);
    }, "key"));
    
    // wait for the task to commence
    Threads.await(startBarrier);
    
    // shut down the executor with one pending task that has commenced
    assertEquals(1, executor.getPendingTasks());
    executor.shutdown();
    
    // should time out, since the task is still blocked
    assertFalse(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
    
    // unblock the task, allowing it to finish
    Threads.await(endBarrier);
    
    task.awaitDone(10, TimeUnit.SECONDS);

    assertFalse(task.isCancelled());
    assertTrue(task.isCompletedSuccessfully());
    assertFalse(task.isCompletedExceptionally());
    
    // should succeed, as the task has been unblocked
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertExecutorCleanedUp();
  }
  
  /**
   *  Awaits for pending tasks after executor shutdown, where the task eventually completes
   *  with an error.
   *
   *  @throws InterruptedException
   */
  @Test
  public void testShutdownAndAwaitPendingTasksWithError() throws InterruptedException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    final LinearFutureTask<?> task = executor.submit(LinearRunnable.decorate(() -> {
      Threads.await(startBarrier);
      Threads.await(endBarrier); // will be interrupted
      assertTrue(Thread.interrupted());
      throw new RuntimeException("Simulated error");
    }, "key"));
    
    // wait for the task to commence
    Threads.await(startBarrier);
    
    // shut down the executor with one pending task that has commenced
    assertEquals(1, executor.getPendingTasks());
    executor.shutdownNow();
    
    // should succeed as the task would have completed (albeit with an error)
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertTrue(task.isCancelled());
    assertExecutorCleanedUp();
  }
  
  /**
   *  Tests the repeat cancellation of a task, specifically that a redundant 
   *  cancellation has no adverse affects.
   *  
   *  @throws InterruptedException
   *  @throws TimeoutException
   */
  @Test
  public void testCancelTaskTwice() throws InterruptedException, TimeoutException {
    final CountingAgentListener agentCounter = new CountingAgentListener();
    executor = new ActorExecutor(new ExecutorOptions()
                                  .withParallelism(1)
                                  .withAgentListener(agentCounter));
    
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    final LinearFutureTask<?> task = executor.submit(LinearRunnable.decorate(() -> {
      Threads.await(startBarrier);
      Threads.await(endBarrier); // will be interrupted
      assertTrue(Thread.interrupted());
      throw new RuntimeException("Simulated error");
    }, "key"));
    
    // wait for the task to commence
    Threads.await(startBarrier);
    
    assertTrue(task.cancel(true));  // cancel task with an interrupt and wait for completion
    task.awaitDone(10, TimeUnit.SECONDS);
    assertTrue(task.isDone());
    assertTrue(task.isCancelled());
    assertFalse(task.isCompletedSuccessfully());
    assertTrue(task.isCompletedExceptionally());
    
    assertFalse(task.cancel(true)); // second cancel will fail
    
    executor.shutdown();
    
    // should succeed as the task would have completed
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertTrue(task.isCancelled());
    assertExecutorCleanedUp();
    
    assertEquals(1, agentCounter.getCounts().size());
    assertEquals(Collections.emptyMap(), agentCounter.getPendingCounts());
  }

  /**
   *  Tests a scenario where a task completes with an error.
   *  
   *  @throws InterruptedException
   *  @throws TimeoutException
   */
  @Test
  public void testTaskError() throws InterruptedException, TimeoutException {
    executor = new ActorExecutor(new ExecutorOptions().withParallelism(1));
    
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    final LinearFutureTask<?> task = executor.submit(LinearRunnable.decorate(() -> {
      Threads.await(startBarrier);
      Threads.await(endBarrier);
      throw new RuntimeException("Simulated error");
    }, "key"));
    
    // wait for the task to commence
    Threads.await(startBarrier);
    assertFalse(task.isCancelled());
    assertFalse(task.isCompletedSuccessfully());
    assertFalse(task.isCompletedExceptionally());
    
    // allow the task to complete with an error
    Threads.await(endBarrier);
    
    task.awaitDone(10, TimeUnit.SECONDS);

    assertFalse(task.isCancelled());
    assertFalse(task.isCompletedSuccessfully());
    assertTrue(task.isCompletedExceptionally());
    
    executor.shutdown();
    
    // should succeed as the task would have completed (albeit with an error)
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    assertExecutorCleanedUp();
  }
  
  private static void randomSleep(int minMillis, int maxMillis) {
    Threads.sleep(minMillis + (int) (Math.random() * (maxMillis - minMillis + 1)));
  }
  
  private static void assertAllSucceeded(List<Future<?>> tasks) {
    for (Future<?> task : tasks) {
      try {
        task.get(0, TimeUnit.MILLISECONDS);
      } catch (ExecutionException e) {
        throw new AssertionError("Task failed " + task, e.getCause());
      } catch (InterruptedException e) {
        throw new AssertionError("Unexpected interrupt", e);
      } catch (TimeoutException e) {
        throw new AssertionError("Task timed out " + task, e);
      }
    }
  }
  
  private static void assertAllCancelled(List<Future<?>> tasks) {
    for (Future<?> task : tasks) {
      assertTrue("for task " + task, task.isCancelled());
    }
  }
  
  private static long sumTaskResults(List<Future<Integer>> tasks) {
    return Exceptions.wrap(() -> {
      long sum = 0L;
      for (Future<Integer> task : tasks) {
        sum += task.get();
      }
      return sum;
    }, RuntimeException::new);
  }
  
  private static long sumInts(AtomicIntegerArray ints) {
    final int length = ints.length();
    long sum = 0L;
    for (int i = 0; i < length; i++) {
      sum += ints.get(i);
    }
    return sum;
  }
}
