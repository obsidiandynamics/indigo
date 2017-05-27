package com.obsidiandynamics.indigo.task;

import static java.util.concurrent.TimeUnit.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class TaskSchedulerTest implements TestSupport {
  private static final class TestTask extends Task<UUID> {
    private final Receiver receiver;
    
    TestTask(long time, UUID id, Receiver receiver) {
      super(time, id);
      this.receiver = receiver;
    }
    
    @Override
    protected void execute() {
      receiver.receive(getId());
    }
  }
  
  private static final class Receiver {
    private final List<UUID> ids = new CopyOnWriteArrayList<>();
    
    void receive(UUID id) {
      ids.add(id);
    }
    
    Callable<Boolean> isSize(int size) {
      return () -> ids.size() == size;
    }
  }
  
  private Receiver receiver;
  
  private TaskScheduler scheduler;
  
  @Before
  public void setup() {
    receiver = new Receiver();
    scheduler = new TaskScheduler("TestTaskScheduler");
    scheduler.start();
  }
  
  @After
  public void teardown() throws InterruptedException {
    scheduler.terminate();
  }
  
  @Test
  public void testSchedule() {
    testSchedule(10);
  }
  
  private void testSchedule(int tasks) {
    final List<UUID> ids = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TestTask task = doIn(i);
      ids.add(task.getId());
      scheduler.schedule(task);
    }
   
    await().atMost(10, SECONDS).until(receiver.isSize(tasks));
    assertEquals(ids, receiver.ids);
  }
  
  @Test
  public void testScheduleInterrupted() {
    final TestTask task = doIn(1);
    // interruption should abort delivery and terminate the thread
    scheduler.interrupt();
    scheduler.schedule(task);
    assertEquals(0, receiver.ids.size());
    await().atMost(10, SECONDS).until(() -> ! scheduler.isAlive());
  }

  @Test
  public void testForceExecute() {
    testForceExecute(10);
  }
  
  private void testForceExecute(int tasks) {
    final List<UUID> ids = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TestTask task = doIn(60_000 + i * 1_000);
      ids.add(task.getId());
      scheduler.schedule(task);
    }
   
    assertEquals(0, receiver.ids.size());
    scheduler.forceExecute();
    await().atMost(10, SECONDS).until(receiver.isSize(tasks));
    assertEquals(ids, receiver.ids);
  }
  
  @Test(expected=InterruptedException.class)
  public void testInterruptedShutdown() throws InterruptedException {
    Thread.currentThread().interrupt();
    scheduler.terminate();
  }
  
  @Test
  public void testAbort() {
    testAbort(10);
  }
  
  private void testAbort(int tasks) {
    final List<TestTask> timeouts = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TestTask task = doIn(60_000 + i * 1_000);
      timeouts.add(task);
      scheduler.schedule(task);
    }
    
    assertEquals(0, receiver.ids.size());
    
    for (TestTask task : timeouts) {
      assertTrue(scheduler.abort(task));
      assertFalse(scheduler.abort(task)); // 2nd call should have no effect
    }
    
    assertEquals(0, receiver.ids.size());
    scheduler.forceExecute();
    assertEquals(0, receiver.ids.size());
  }

  @Test
  public void testEarlyExecute() {
    testEarlyExecute(10);
  }
  
  private void testEarlyExecute(int tasks) {
    final List<TestTask> timeouts = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TestTask task = doIn(60_000 + i * 1_000);
      timeouts.add(task);
      scheduler.schedule(task);
    }
   
    assertEquals(0, receiver.ids.size());
    for (TestTask task : timeouts) {
      scheduler.executeNow(task);
      scheduler.executeNow(task); // 2nd call should have no effect
    }
    assertEquals(tasks, receiver.ids.size());
    scheduler.forceExecute();
    assertEquals(tasks, receiver.ids.size());
  }
  
  private TestTask doIn(long millis) {
    return new TestTask(System.nanoTime() + millis * 1_000_000l, 
                        UUID.randomUUID(),
                        receiver);
  }
}
