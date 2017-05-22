package com.obsidiandynamics.indigo.ws;

import static junit.framework.TestCase.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.awaitility.*;
import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.thread.*;
import org.junit.*;
import org.xnio.*;

import com.obsidiandynamics.indigo.*;
import com.obsidiandynamics.indigo.ws.fake.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class WSFanOutTest implements TestSupport {
  private static boolean LOG_TIMINGS = true;
  public static boolean LOG_1K = true;
  private static boolean LOG_PHASES = true;
  
  private static final int PORT = 6667;
  private static final int IDLE_TIMEOUT = 0;
  
  private static final int N = 100;           // number of outgoing messages per connection
  private static final int M = 10;            // number of connections
  private static final boolean ECHO = false;  // whether the client should respond to a broadcast
  private static final int BYTES = 16;        // bytes per message
  private static final int CYCLES = 1;        // number of repeats
  private static final boolean FLUSH = false; // whether the messages should be flushed on the server after enqueuing
  
  private static int totalConnected(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.connected.get() ? 1 : 0).sum();
  }
  
  private static int totalClosed(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.closed.get() ? 1 : 0).sum();
  }
  
  private static int totalReceived(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.received.get()).sum();
  }
  
  private static int totalSent(List<ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.sent.get()).sum();
  }
  
  private static XnioWorker getXnioWorker() throws IllegalArgumentException, IOException {
    return Xnio.getInstance().createWorker(OptionMap.builder()
                                           .set(Options.WORKER_IO_THREADS, Runtime.getRuntime().availableProcessors())
                                           .set(Options.CONNECTION_HIGH_WATER, 1000000)
                                           .set(Options.CONNECTION_LOW_WATER, 1000000)
                                           .set(Options.WORKER_TASK_CORE_THREADS, 100)
                                           .set(Options.WORKER_TASK_MAX_THREADS, 10_000)
                                           .set(Options.TCP_NODELAY, true)
                                           .getMap());
  }
  
  @Test
  public void testNtUt() throws Exception {
    final XnioWorker worker = getXnioWorker();
    test(N, M, ECHO, BYTES, CYCLES,
         NettyServerHarness.factory(PORT, IDLE_TIMEOUT),
         UndertowClientHarness.factory(worker, PORT, IDLE_TIMEOUT, ECHO),
         worker::shutdown);
  }
  
  @Test
  public void testUtUt() throws Exception {
    final XnioWorker worker = getXnioWorker();
    test(N, M, ECHO, BYTES, CYCLES,
         UndertowServerHarness.factory(PORT, IDLE_TIMEOUT),
         UndertowClientHarness.factory(worker, PORT, IDLE_TIMEOUT, ECHO),
         worker::shutdown);
  }
  
  @Test
  public void testUtFc() throws Exception {
    test(N, M, ECHO, BYTES, CYCLES,
         UndertowServerHarness.factory(PORT, IDLE_TIMEOUT),
         FakeClientHarness.factory(PORT, BYTES),
         ThrowingRunnable::noOp);
  }
  
  @Test
  public void testJtJt() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(100));
    httpClient.start();
    test(N, M, ECHO, BYTES, CYCLES,
         JettyServerHarness.factory(PORT, IDLE_TIMEOUT),
         JettyClientHarness.factory(httpClient, PORT, IDLE_TIMEOUT, ECHO),
         httpClient::stop);
  }
  
  @Test
  public void testUtJt() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(10_000, 100));
    httpClient.start();
    test(N, M, ECHO, BYTES, CYCLES,
         UndertowServerHarness.factory(PORT, IDLE_TIMEOUT),
         JettyClientHarness.factory(httpClient, PORT, IDLE_TIMEOUT, ECHO),
         httpClient::stop);
  }
  
  @Test
  public void testJtUt() throws Exception {
    final XnioWorker worker = getXnioWorker();
    test(N, M, ECHO, BYTES, CYCLES,
         JettyServerHarness.factory(PORT, IDLE_TIMEOUT),
         UndertowClientHarness.factory(worker, PORT, IDLE_TIMEOUT, ECHO),
         worker::shutdown);
  }
  
  private <E> void test(int n, int m, boolean echo, int numBytes, int cycles,
                        ThrowingFactory<? extends ServerHarness<E>> serverHarnessFactory,
                        ThrowingFactory<? extends ClientHarness> clientHarnessFactory,
                        ThrowingRunnable cleanup) throws Exception {
    for (int i = 0; i < cycles; i++) {
      test(n, m, echo, numBytes, serverHarnessFactory, clientHarnessFactory, cleanup);
    }
  }
  
  private <E> void test(int n, int m, boolean echo, int numBytes,
                        ThrowingFactory<? extends ServerHarness<E>> serverHarnessFactory,
                        ThrowingFactory<? extends ClientHarness> clientHarnessFactory,
                        ThrowingRunnable cleanup) throws Exception {
    final int sendThreads = 1;
    final int waitScale = 1 + n * m / 1_000_000;
    
    final ServerHarness<E> server = serverHarnessFactory.create();
    final List<ClientHarness> clients = new ArrayList<>(m);
    
    for (int i = 0; i < m; i++) {
      clients.add(clientHarnessFactory.create()); 
    }

    if (LOG_PHASES) System.out.println("s: awaiting server.connected");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.connected.get() == m);

    assertEquals(m, server.connected.get());
    assertEquals(m, totalConnected(clients));

    final byte[] bytes = new byte[numBytes];
    new Random().nextBytes(bytes);
    final long start = System.currentTimeMillis();
    
    final List<E> endpoints = server.getEndpoints();
    ParallelJob.blockingSlice(endpoints, sendThreads, sublist -> {
      for (int i = 0; i < n; i++) {
        if (LOG_1K && i % 1000 == 0) System.out.println("s: queued " + i);
        server.broadcast(sublist, bytes);
      }
      if (FLUSH) {
        if (LOG_PHASES) System.out.println("s: flushing");
        for (int i = 0; i < n; i++) {
          try {
            server.flush(sublist);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (LOG_PHASES) System.out.println("s: flushed");
      }
    }).run();

    if (LOG_PHASES) System.out.println("s: awaiting server.sent");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.sent.get() >= m * n);
    assertEquals(m * n, server.sent.get());

    if (LOG_PHASES) System.out.println("s: awaiting client.received");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalReceived(clients) >= m * n);
    assertEquals(m * n, totalReceived(clients));

    if (LOG_PHASES) System.out.println("s: awaiting client.sent");
    if (echo) {
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalSent(clients) >= m * n);
      assertEquals(m * n, totalSent(clients));
    } else {
      assertEquals(0, totalSent(clients));
    }
    
    if (LOG_TIMINGS) {
      final long tookMillis = System.currentTimeMillis() - start;
      final float ratePerSec = 1000f * n * m / tookMillis * (echo ? 2 : 1);
      final float bandwidthMpbs = ratePerSec * numBytes / (1 << 20);
      LOG_STREAM.format("took %,d ms, %,.0f/s, %,.1f Mb/s (%d threads active)\n", 
                        tookMillis, ratePerSec, bandwidthMpbs, Thread.activeCount());
    }

    for (ClientHarness client : clients) {
      client.close();
    }

    if (LOG_PHASES) System.out.println("s: awaiting server.closed");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.closed.get() == m);
    assertEquals(m, server.closed.get());

    if (LOG_PHASES) System.out.println("s: awaiting client.closed");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalClosed(clients) == m);
    assertEquals(m, totalClosed(clients));

    if (echo) {
      if (LOG_PHASES) System.out.println("s: awaiting server.received");
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.received.get() == m * n);
      assertEquals(m * n, server.received.get());
    } else {
      assertEquals(0, server.received.get());
    }

    server.close();
    cleanup.run();
  }
}
