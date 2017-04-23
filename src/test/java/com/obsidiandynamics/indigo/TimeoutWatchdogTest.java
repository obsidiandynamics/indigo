package com.obsidiandynamics.indigo;

import static java.util.concurrent.TimeUnit.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

public final class TimeoutWatchdogTest implements TestSupport {
  private final class MockEndpoint implements Endpoint {
    private final List<UUID> ids = new CopyOnWriteArrayList<>();
    
    @Override
    public void send(Message message) {
      ids.add(message.requestId());
    }
    
    Callable<Boolean> isSize(int size) {
      return () -> ids.size() == size;
    }
  }
  
  private MockEndpoint endpoint;
  
  private TimeoutWatchdog watchdog;
  
  @Before
  public void setup() {
    endpoint = new MockEndpoint();
    watchdog = new TimeoutWatchdog(endpoint);
    watchdog.start();
  }
  
  @After
  public void teardown() {
    watchdog.terminate();
  }
  
  @Test
  public void testTimeout() {
    testTimeout(10);
  }
  
  private void testTimeout(int tasks) {
    final List<UUID> ids = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TimeoutTask task = expireIn(i);
      ids.add(task.getRequestId());
      watchdog.schedule(task);
    }
   
    await().atMost(10, SECONDS).until(endpoint.isSize(tasks));
    assertEquals(ids, endpoint.ids);
  }

  @Test
  public void testForceTimeout() {
    testForceTimeout(10);
  }
  
  private void testForceTimeout(int tasks) {
    final List<UUID> ids = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TimeoutTask task = expireIn(60_000 + i * 1_000);
      ids.add(task.getRequestId());
      watchdog.schedule(task);
    }
   
    assertEquals(0, endpoint.ids.size());
    watchdog.forceTimeout();
    await().atMost(10, SECONDS).until(endpoint.isSize(tasks));
    assertEquals(ids, endpoint.ids);
  }
  
  @Test
  public void testInterruptedShutdown() {
    Thread.currentThread().interrupt();
    watchdog.terminate();
    assertTrue(Thread.interrupted());
  }
  
  @Test
  public void testDequeue() {
    testDequeue(10);
  }
  
  private void testDequeue(int tasks) {
    final List<TimeoutTask> timeouts = new ArrayList<>(tasks); 
    for (int i = 0; i < tasks; i++) {
      final TimeoutTask task = expireIn(60_000 + i * 1_000);
      timeouts.add(task);
      watchdog.schedule(task);
    }
    
    assertEquals(0, endpoint.ids.size());
    
    for (TimeoutTask task : timeouts) {
      watchdog.abort(task);
    }
    
    assertEquals(0, endpoint.ids.size());
    watchdog.forceTimeout();
    assertEquals(0, endpoint.ids.size());
  }
  
  private static TimeoutTask expireIn(long millis) {
    return new TimeoutTask(System.nanoTime() + millis * 1_000_000l, 
                           UUID.randomUUID(),
                           ActorRef.of("test"),
                           new PendingRequest(null, null, null));
  }
}
