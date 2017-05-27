package com.obsidiandynamics.indigo.messagebus;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ZmqMessageBusTest implements TestSupport {
  @Test
  public void testSendReceiveSync() throws InterruptedException {
    final int cycles = 100;
    final int nPerCycle = 10;
    for (int i = 0; i < cycles; i++) {
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
  
  @Test
  public void testSendReceiveAsync() {
    final int n = 10;
    final MessageBus bus = new ZmqMessageBus("tcp://*:5557", new StringCodec());

    final MessagePublisher pub = bus.getPublisher("test");
    
    final AtomicInteger received = new AtomicInteger();
    final AtomicBoolean synced = new AtomicBoolean();
    bus.getSubscriber("test").onReceive(msg -> {
      if (msg.equals("sync")) {
        log("s: synced\n");
        synced.set(true);
      } else {
        received.incrementAndGet();
        log("s: received '%s'\n", msg);
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
    
    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> received.get() >= n);
    assertEquals(n, received.get());
    bus.close();
  }
}
