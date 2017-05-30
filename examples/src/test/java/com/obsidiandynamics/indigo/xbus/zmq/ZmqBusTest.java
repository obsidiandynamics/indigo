package com.obsidiandynamics.indigo.xbus.zmq;

import static junit.framework.TestCase.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.junit.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.xbus.*;
import com.obsidiandynamics.indigo.xbus.codec.*;

public final class ZmqBusTest implements TestSupport {
  private static final int PROGRESS_INTERVAL = 100;
  private static final int SCALE = 1;
  
  private Thread syncThread(XBus bus, AtomicBoolean synced) {
    return Threads.asyncDaemon(() -> {
      final XPublisher pub = bus.getPublisher("test");
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
    }, "ZmqMessageBusTest-Sync");
  }
  
  private void sendParallel(XBus bus, int n, int pubThreads) {
    ParallelJob.blockingSlice(Arrays.asList(new Object[pubThreads]), pubThreads, o -> {
      final XPublisher pub = bus.getPublisher("test");
      for (int i = 0; i < n; i++) {
        log("p: sending\n");
        pub.send("hello");
      }
    }).run();
  }
  
  @Test
  public void testSendReceiveSync() throws InterruptedException {
    final int cycles = 10 * sqrtScale();
    final int nPerThreadCycle = 100 * sqrtScale();
    final int pubThreads = 4;
    for (int i = 0; i < cycles; i++) {
      if (i % PROGRESS_INTERVAL == PROGRESS_INTERVAL - 1) LOG_STREAM.format("testSendReceiveSync: %,d cycles\n", i);
      testSendReceiveSync(nPerThreadCycle, pubThreads);
    }
  }
  
  private void testSendReceiveSync(int n, int pubThreads) throws InterruptedException {
    final XBus bus = new ZmqBus("tcp://*:5557", new StringCodec());
    
    final AtomicInteger received = new AtomicInteger();
    final AtomicBoolean synced = new AtomicBoolean();
    final Thread subThread = Threads.asyncDaemon(() -> {
      final XSubscriber sub = bus.getSubscriber("test");
      log("s: starting\n");
      while (received.get() != n) {
        final Object r = sub.receive();
        if (r.equals("sync")) {
          log("s: synced\n");
          synced.set(true);
        } else {
          received.incrementAndGet();
          log("s: received '%s'\n", r);
        }
      }
      sub.close();
    }, "ZmqMessageBusTest-subscriber");
    
    syncThread(bus, synced).join();

    sendParallel(bus, n, pubThreads);

    subThread.join(10_000); // allow time for the subscriber to receive all messages and wind up
    
    assertEquals(n, received.get());
    bus.close();
  }
  
  private static int sqrtScale() {
    return (int) Math.max(1, Math.sqrt(SCALE));
  }
  
  @Test
  public void testSendReceiveAsync() throws InterruptedException {
    final int cycles = 2 * sqrtScale();
    final int nPerCycle = 100 * sqrtScale();
    final int pubThreads = 4;
    for (int i = 0; i < cycles; i++) {
      if (i % PROGRESS_INTERVAL == PROGRESS_INTERVAL - 1) LOG_STREAM.format("testSendReceiveAsync: %d cycles\n", i);
      testSendReceiveAsync(nPerCycle, pubThreads);
    }
  }
  
  private void testSendReceiveAsync(int n, int pubThreads) throws InterruptedException {
    final XBus bus = new ZmqBus("tcp://*:5557", new StringCodec());
    
    final AtomicInteger received = new AtomicInteger();
    final AtomicBoolean synced = new AtomicBoolean();
    AsyncSubscriber
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
    
    syncThread(bus, synced).join();

    sendParallel(bus, n, pubThreads);
    
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> received.get() >= n * pubThreads);
    assertEquals(n * pubThreads, received.get());
    bus.close();
  }
}
