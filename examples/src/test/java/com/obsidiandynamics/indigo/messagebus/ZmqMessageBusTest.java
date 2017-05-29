package com.obsidiandynamics.indigo.messagebus;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ZmqMessageBusTest implements TestSupport {
  private static final int PROGRESS_INTERVAL = 100;
  private static final int SCALE = 1;
  
  @Test
  public void testSendReceiveSync() throws InterruptedException {
    final int cycles = 10 * sqrtScale();
    final int nPerCycle = 100 * sqrtScale();
    for (int i = 0; i < cycles; i++) {
      if (i % PROGRESS_INTERVAL == PROGRESS_INTERVAL - 1) LOG_STREAM.format("testSendReceiveSync: %d cycles\n", i);
      testSendReceiveSync(nPerCycle);
    }
  }
  
  private void testSendReceiveSync(int n) throws InterruptedException {
    final MessageBus bus = new ZmqMessageBus("tcp://*:5557", new StringCodec());
    
    final List<Object> received = new ArrayList<>();

    final AtomicBoolean synced = new AtomicBoolean();
    final MessagePublisher pub = bus.getPublisher("test");
    final Thread subThread = Threads.asyncDaemon(() -> {
      final MessageSubscriber sub = bus.getSubscriber("test");
      log("s: starting\n");
      while (received.size() != n) {
        final Object r = sub.receive();
        if (r.equals("sync")) {
          log("s: synced\n");
          synced.set(true);
        } else {
          received.add(r);
          log("s: received '%s'\n", r);
        }
      }
      sub.close();
    }, "ZmqSubscriberThread");
    
    final long syncStart = System.currentTimeMillis();
    final long maxSyncWait = 10_000;
    while (! synced.get()) {
      log("p: syncing\n");
      pub.send("sync");
      TestSupport.sleep(1);
      final long taken = System.currentTimeMillis() - syncStart;
      assertTrue("sync is taking " + taken + " ms", taken < maxSyncWait);
    }
    log("s: sync took %d ms\n", System.currentTimeMillis() - syncStart);

    for (int i = 0; i < n; i++) {
      log("p: sending\n");
      pub.send("hello");
    }
    
    subThread.join(10_000); // allow time for the subscriber to receive all messages and wind up
    
    assertEquals(n, received.size());
    bus.close();
  }
  
  private static int sqrtScale() {
    return (int) Math.max(1, Math.sqrt(SCALE));
  }
  
  @Test
  public void testSendReceiveAsync() {
    final int cycles = 2 * sqrtScale();
    final int nPerCycle = 100 * sqrtScale();
    for (int i = 0; i < cycles; i++) {
      if (i % PROGRESS_INTERVAL == PROGRESS_INTERVAL - 1) LOG_STREAM.format("testSendReceiveAsync: %d cycles\n", i);
      testSendReceiveAsync(nPerCycle);
    }
  }
  
  private void testSendReceiveAsync(int n) {
    final MessageBus bus = new ZmqMessageBus("tcp://*:5557", new StringCodec());

    final MessagePublisher pub = bus.getPublisher("test");
    
    final AtomicInteger received = new AtomicInteger();
    final AtomicBoolean synced = new AtomicBoolean();
    AsyncMessageSubscriber
    .using(() -> bus.getSubscriber("test"))
    .onReceive(msg -> {
      if (msg.equals("sync")) {
        log("s: synced\n");
        synced.set(true);
      } else {
        final int r = received.incrementAndGet();
        log("s: received '%s' (%d)\n", msg, r);
      }
    });
    
    final long syncStart = System.currentTimeMillis();
    final long maxSyncWait = 10_000;
    while (! synced.get()) {
      log("p: syncing\n");
      pub.send("sync");
      TestSupport.sleep(1);
      final long taken = System.currentTimeMillis() - syncStart;
      assertTrue("sync is taking " + taken + " ms", taken < maxSyncWait);
    }
    log("s: sync took %d ms\n", System.currentTimeMillis() - syncStart);

    for (int i = 0; i < n; i++) {
      log("p: sending\n");
      pub.send("hello");
    }
    
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> received.get() >= n);
    assertEquals(n, received.get());
    bus.close();
  }
}
