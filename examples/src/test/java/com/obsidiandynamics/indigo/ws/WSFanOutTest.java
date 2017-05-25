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
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.fake.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class WSFanOutTest implements TestSupport {
  private static boolean LOG_TIMINGS = true;
  public static boolean LOG_1K = false;
  private static boolean LOG_PHASES = true;
  
  private static final int PORT = 6667;
  private static final int IDLE_TIMEOUT = 0;
  
  private static final int N = 100;           // number of outgoing messages per connection
  private static final int M = 10;            // number of connections
  private static final boolean ECHO = false;  // whether the client should respond to a broadcast
  private static final int BYTES = 16;        // bytes per message
  private static final int CYCLES = 1;        // number of repeats
  private static final boolean FLUSH = false; // flush on the server after enqueuing (if 'nodelay' is disabled)
  
  private static final int BACKLOG_LWM = 1_000_000;
  
  private static final int UT_CLIENT_BUFFER_SIZE = Math.max(1024, BYTES);
  
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
                                           .set(Options.THREAD_DAEMON, true)
                                           .set(Options.CONNECTION_HIGH_WATER, 1_000_000)
                                           .set(Options.CONNECTION_LOW_WATER, 1_000_000)
                                           .set(Options.WORKER_TASK_CORE_THREADS, 100)
                                           .set(Options.WORKER_TASK_MAX_THREADS, 10_000)
//                                           .set(Options.RECEIVE_BUFFER, 1024)
                                           .set(Options.TCP_NODELAY, true)
                                           .getMap());
  }
  
  private static <E extends WSEndpoint<E>> ThrowingSupplier<DefaultServerHarness<E>> serverHarnessFactory(WSServerFactory<E> serverFactory) throws Exception {
    return () -> new DefaultServerHarness<>(new WSConfig() {{
      port = PORT;
      contextPath = "/";
      idleTimeoutMillis = IDLE_TIMEOUT;
    }}, serverFactory);
  }
  
  @Test
  public void testNtUt() throws Exception {
    final XnioWorker worker = getXnioWorker();
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(NettyServer.factory()),
         UndertowClientHarness.factory(worker, PORT, IDLE_TIMEOUT, UT_CLIENT_BUFFER_SIZE, ECHO),
         worker::shutdown);
  }
  
  @Test
  public void testUtUt() throws Exception {
    final XnioWorker worker = getXnioWorker();
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(UndertowServer.factory()),
         UndertowClientHarness.factory(worker, PORT, IDLE_TIMEOUT, UT_CLIENT_BUFFER_SIZE, ECHO),
         worker::shutdown);
  }
  
  @Test
  public void testUtFc() throws Exception {
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(UndertowServer.factory()),
         FakeClientHarness.factory(PORT, BYTES),
         ThrowingRunnable::noOp);
  }
  
  @Test
  public void testJtJt() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(100));
    httpClient.start();
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(JettyServer.factory()),
         JettyClientHarness.factory(httpClient, PORT, IDLE_TIMEOUT, ECHO),
         httpClient::stop);
  }
  
  @Test
  public void testUtJt() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(10_000, 100));
    httpClient.start();
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(UndertowServer.factory()),
         JettyClientHarness.factory(httpClient, PORT, IDLE_TIMEOUT, ECHO),
         httpClient::stop);
  }
  
  @Test
  public void testJtUt() throws Exception {
    final XnioWorker worker = getXnioWorker();
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(JettyServer.factory()),
         UndertowClientHarness.factory(worker, PORT, IDLE_TIMEOUT, UT_CLIENT_BUFFER_SIZE, ECHO),
         worker::shutdown);
  }
  
  private void throttle(List<? extends WSEndpoint<?>> endpoints, int backlogLwm) {
    boolean logged = false;
    for (;;) {
      long totalBacklog = 0;
      inner: for (WSEndpoint<?> endpoint : endpoints) {
        totalBacklog += endpoint.getBacklog();
        if (totalBacklog > backlogLwm) {
          break inner;
        }
      }
      
      if (totalBacklog > backlogLwm) {
        if (LOG_PHASES && ! logged) {
          LOG_STREAM.format("s: throttling, backlog is at least %,d\n", totalBacklog);
          logged = true;
        }
        Thread.yield();
      } else {
        break;
      }
    }
  }
  
  private <E extends WSEndpoint<E>> void test(int n, int m, boolean echo, int numBytes, int cycles,
                                              ThrowingSupplier<? extends ServerHarness<E>> serverHarnessFactory,
                                              ThrowingSupplier<? extends ClientHarness> clientHarnessFactory,
                                              ThrowingRunnable cleanup) throws Exception {
    for (int i = 0; i < cycles; i++) {
      test(n, m, echo, numBytes, serverHarnessFactory, clientHarnessFactory);
      if (LOG_PHASES && i < cycles - 1) {
        LOG_STREAM.format("_");
      }
    }
    cleanup.run();
  }
  
  private <E extends WSEndpoint<E>> void test(int n, int m, boolean echo, int numBytes,
                                              ThrowingSupplier<? extends ServerHarness<E>> serverHarnessFactory,
                                              ThrowingSupplier<? extends ClientHarness> clientHarnessFactory) throws Exception {
    final int sendThreads = 1;
    final int waitScale = 1 + (int) (((long) n * (long) m) / 1_000_000_000l);
    
    final ServerHarness<E> server = serverHarnessFactory.create();
    final List<ClientHarness> clients = new ArrayList<>(m);
    
    for (int i = 0; i < m; i++) {
      clients.add(clientHarnessFactory.create()); 
    }

    if (LOG_PHASES) LOG_STREAM.format("s: awaiting server.connected\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.connected.get() == m);

    assertEquals(m, server.connected.get());
    assertEquals(m, totalConnected(clients));

    final byte[] bytes = new byte[numBytes];
    new Random().nextBytes(bytes);
    final long start = System.currentTimeMillis();
    
    final List<E> endpoints = server.getEndpoints();

    if (LOG_PHASES) LOG_STREAM.format("s: sending\n");
    ParallelJob.blockingSlice(endpoints, sendThreads, sublist -> {
      long sent = 0;
      for (int i = 0; i < n; i++) {
        if (LOG_1K && i % 1000 == 0) LOG_STREAM.format("s: queued %d\n", i);
        server.broadcast(sublist, bytes);
        sent += sublist.size();
        
        if (BACKLOG_LWM != 0 && sent > BACKLOG_LWM) {
          sent = 0;
          throttle(endpoints, BACKLOG_LWM);
        }
      }
      
      if (FLUSH) {
        if (LOG_PHASES) LOG_STREAM.format("s: flushing\n");
        for (int i = 0; i < n; i++) {
          try {
            server.flush(sublist);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (LOG_PHASES) LOG_STREAM.format("s: flushed\n");
      }
    }).run();

    if (LOG_PHASES) LOG_STREAM.format("s: awaiting server.sent\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.sent.get() >= m * n);
    assertEquals(m * n, server.sent.get());

    if (LOG_PHASES) LOG_STREAM.format("s: awaiting client.received\n");
    long waitStart = System.currentTimeMillis();
    long lastPrint = System.currentTimeMillis();
    for (;;) {
      final long totalReceived = totalReceived(clients);
      if (totalReceived >= m * n) {
        break;
      } else if (System.currentTimeMillis() - lastPrint > 1000) {
        final long takingSeconds = (System.currentTimeMillis() - waitStart) / 1000;
        if (LOG_PHASES) LOG_STREAM.format("s: ... %,d seconds later %,d received\n", takingSeconds, totalReceived);
        lastPrint = System.currentTimeMillis();
      } else {
        Thread.sleep(10);
      }
    }
    
    assertEquals(m * n, totalReceived(clients));

    if (LOG_PHASES) LOG_STREAM.format("s: awaiting client.sent\n");
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

    if (LOG_PHASES) LOG_STREAM.format("s: awaiting server.closed\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.closed.get() == m);
    assertEquals(m, server.closed.get());

    if (LOG_PHASES) LOG_STREAM.format("s: awaiting client.closed\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalClosed(clients) == m);
    assertEquals(m, totalClosed(clients));

    if (echo) {
      if (LOG_PHASES) LOG_STREAM.format("s: awaiting server.received\n");
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.received.get() == m * n);
      assertEquals(m * n, server.received.get());
    } else {
      assertEquals(0, server.received.get());
    }

    server.close();
  }
  
  public static void main(String[] args) {
    try {
      BashInteractor.Ulimit.main(null);
      new WSFanOutTest().testUtUt();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
