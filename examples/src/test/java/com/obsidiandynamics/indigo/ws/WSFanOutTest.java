package com.obsidiandynamics.indigo.ws;

import static junit.framework.TestCase.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.awaitility.*;
import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.thread.*;
import org.junit.*;
import org.xnio.*;

import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.util.TestSupport;
import com.obsidiandynamics.indigo.util.ParallelJob;
import com.obsidiandynamics.indigo.ws.fake.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class WSFanOutTest implements TestSupport {
  private static boolean LOG_TIMINGS = true;
  public static boolean LOG_1K = false;
  private static boolean LOG_PHASES = true;
  private static boolean LOG_PROGRESS = true;
  private static final int PROGRESS_INTERVAL = 5_000;
  
  private static final int PORT = 6667;
  private static final int IDLE_TIMEOUT = 0;
  
  private static final int N = 100;           // number of outgoing messages per connection
  private static final int M = 10;            // number of connections
  private static final boolean ECHO = false;  // whether the client should respond to a broadcast
  private static final int BYTES = 16;        // bytes per message
  private static final int CYCLES = 1;        // number of repeats
  private static final boolean FLUSH = false; // flush on the server after enqueuing (if 'nodelay' is disabled)
  
  private static final int BACKLOG_HWM = 1_000_000;
  
  private static final int UT_CLIENT_BUFFER_SIZE = Math.max(1024, BYTES);
  
  private static int totalConnected(List<? extends ClientHarness<?>> clients) {
    return clients.stream().mapToInt(c -> c.connected.get() ? 1 : 0).sum();
  }
  
  private static int totalClosed(List<? extends ClientHarness<?>> clients) {
    return clients.stream().mapToInt(c -> c.closed.get() ? 1 : 0).sum();
  }
  
  private static long totalReceived(List<? extends ClientHarness<?>> clients) {
    return clients.stream().mapToLong(c -> c.received.get()).sum();
  }
  
  private static long totalSent(List<? extends ClientHarness<?>> clients) {
    return clients.stream().mapToLong(c -> c.sent.get()).sum();
  }
  
  private static XnioWorker createXnioWorker() throws IllegalArgumentException, IOException {
    return Xnio.getInstance().createWorker(OptionMap.builder()
                                           .set(Options.WORKER_IO_THREADS, Runtime.getRuntime().availableProcessors())
                                           .set(Options.THREAD_DAEMON, true)
                                           .set(Options.CONNECTION_HIGH_WATER, 1_000_000)
                                           .set(Options.CONNECTION_LOW_WATER, 1_000_000)
                                           .set(Options.WORKER_TASK_CORE_THREADS, 100)
                                           .set(Options.WORKER_TASK_MAX_THREADS, 10_000)
                                           .set(Options.TCP_NODELAY, true)
                                           .getMap());
  }
  
  private static HttpClient createHttpClient() throws Exception {
    final HttpClient httpClient = new HttpClient();
    httpClient.setExecutor(new QueuedThreadPool(10_000, 100));
    httpClient.start();
    return httpClient;
  }
  
  private static <E extends WSEndpoint> ServerHarnessFactory<DefaultServerHarness<E>> serverHarnessFactory(WSServerFactory<E> serverFactory) throws Exception {
    return progress -> new DefaultServerHarness<>(new WSServerConfig() {{
      port = PORT;
      contextPath = "/";
      idleTimeoutMillis = IDLE_TIMEOUT;
    }}, serverFactory, progress);
  }
  
  private static <E extends WSEndpoint> ClientHarnessFactory<DefaultClientHarness<E>> clientHarnessFactory(WSClient<E> client) {
    return () -> new DefaultClientHarness<>(client, PORT, ECHO);
  }
  
  private static <E extends WSEndpoint> WSClient<E> createClient(WSClientFactory<E> clientFactory) throws Exception {
    return clientFactory.create(new WSClientConfig() {{
      idleTimeoutMillis = IDLE_TIMEOUT;
    }});
  }

  private static ClientHarnessFactory<FakeClientHarness> fakeClientFactory() {
    return () -> new FakeClientHarness(PORT, BYTES);
  }
  
  @Test
  public void testNtUt() throws Exception {
    final WSClient<UndertowEndpoint> client = createClient(UndertowClient.factory(createXnioWorker(), UT_CLIENT_BUFFER_SIZE));
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(NettyServer.factory()),
         clientHarnessFactory(client),
         client::close);
  }
  
  @Test
  public void testUtUt() throws Exception {
    final WSClient<UndertowEndpoint> client = createClient(UndertowClient.factory(createXnioWorker(), UT_CLIENT_BUFFER_SIZE));
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(UndertowServer.factory()),
         clientHarnessFactory(client),
         client::close);
  }
  
  @Test
  public void testUtFc() throws Exception {
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(UndertowServer.factory()),
         fakeClientFactory(),
         ThrowingRunnable::noOp);
  }
  
  @Test
  public void testJtJt() throws Exception {
    final WSClient<JettyEndpoint> client = createClient(JettyClient.factory(createHttpClient()));
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(JettyServer.factory()),
         clientHarnessFactory(client),
         client::close);
  }
  
  @Test
  public void testUtJt() throws Exception {
    final WSClient<JettyEndpoint> client = createClient(JettyClient.factory(createHttpClient()));
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(UndertowServer.factory()),
         clientHarnessFactory(client),
         client::close);
  }
  
  @Test
  public void testJtUt() throws Exception {
    final WSClient<UndertowEndpoint> client = createClient(UndertowClient.factory(createXnioWorker(), UT_CLIENT_BUFFER_SIZE));
    test(N, M, ECHO, BYTES, CYCLES,
         serverHarnessFactory(JettyServer.factory()),
         clientHarnessFactory(client),
         client::close);
  }
  
  private void throttle(AtomicBoolean throttleInProgress, List<? extends WSEndpoint> endpoints, int backlogHwm) {
    boolean logged = false;
    int waits = 0;
    for (;;) {
      long minTotalBacklog = 0;
      boolean overflow = false;
      inner: for (WSEndpoint endpoint : endpoints) {
        minTotalBacklog += endpoint.getBacklog();
        if (minTotalBacklog > backlogHwm) {
          overflow = true;
          break inner;
        }
      }
      
      if (overflow) {
        throttleInProgress.set(true);
        if (LOG_PHASES && ! logged) {
          LOG_STREAM.format("s: throttling", minTotalBacklog);
          logged = true;
        }
        if (logged && ++waits % 1000 == 0) {
          LOG_STREAM.format(".");
        }
        TestSupport.sleep(1);
      } else if (minTotalBacklog < backlogHwm / 2) {
        if (logged) {
          LOG_STREAM.format("\n");
          throttleInProgress.set(false);
        }
        break;
      }
    }
  }
  
  private <S extends WSEndpoint, C extends WSEndpoint> void test(int n, int m, boolean echo, int numBytes, int cycles,
                                                                 ServerHarnessFactory<? extends ServerHarness<S>> serverHarnessFactory,
                                                                 ClientHarnessFactory<? extends ClientHarness<C>> clientHarnessFactory,
                                                                 ThrowingRunnable cleanup) throws Exception {
    for (int i = 0; i < cycles; i++) {
      test(n, m, echo, numBytes, serverHarnessFactory, clientHarnessFactory);
      if (LOG_PHASES && i < cycles - 1) {
        LOG_STREAM.format("_\n");
      }
    }
    cleanup.run();
  }
  
  private <S extends WSEndpoint, C extends WSEndpoint> void test(int n, int m, boolean echo, int numBytes,
                                                                 ServerHarnessFactory<? extends ServerHarness<S>> serverHarnessFactory,
                                                                 ClientHarnessFactory<? extends ClientHarness<C>> clientHarnessFactory) throws Exception {
    final int sendThreads = 1;
    final int waitScale = 1 + (int) (((long) n * (long) m) / 1_000_000_000l);
    final List<ClientHarness<C>> clients = new ArrayList<>(m);
    final AtomicBoolean throttleInProgress = new AtomicBoolean();
    
    final ServerProgress progress = new ServerProgress() {
      private final AtomicBoolean updateInProgress = new AtomicBoolean();
      private long firstUpdate;
      private long lastUpdate;
      private long lastSent;
      private long lastReceived;
      @Override public void update(ServerHarness<?> server, long sent) {
        if (! LOG_PROGRESS) return;
        
        final long now = System.currentTimeMillis();
        if (sent == 0) {
          firstUpdate = lastUpdate = now;
          return;
        }
        
        final long timeDelta = now - lastUpdate;
        if (timeDelta < PROGRESS_INTERVAL) return;
        
        if (updateInProgress.compareAndSet(false, true)) {
          try {
            final long time = now - firstUpdate;
            final long received = totalReceived(clients);
            sent = server.sent.get();
            final long txDelta = sent - lastSent;
            final long rxDelta = received - lastReceived;
            final float txAverageRate = 1000f * sent / time;
            final float txCurrentRate = 1000f * txDelta / timeDelta;
            final float rxAverageRate = 1000f * received / time;
            final float rxCurrentRate = 1000f * rxDelta / timeDelta;
            lastUpdate = now;
            lastSent = sent;
            lastReceived = received;
            
            if (! throttleInProgress.get()) {
              LOG_STREAM.format("> tx: %,d, cur: %,.0f/s, avg: %,.0f/s\n", sent, txCurrentRate, txAverageRate);
              LOG_STREAM.format("< rx: %,d, cur: %,.0f/s, avg: %,.0f/s\n", received, rxCurrentRate, rxAverageRate);
            }
          } finally {
            updateInProgress.set(false);
          }
        }
      }
    };
    
    final ServerHarness<S> server = serverHarnessFactory.create(progress);
    
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
    
    final List<S> endpoints = server.getEndpoints();

    if (LOG_PHASES) LOG_STREAM.format("s: sending\n");
    ParallelJob.blockingSlice(endpoints, sendThreads, sublist -> {
      long sent = 0;
      for (int i = 0; i < n; i++) {
        if (LOG_1K && i % 1000 == 0) LOG_STREAM.format("s: queued %d\n", i);
        server.broadcast(sublist, bytes);
        
        if (BACKLOG_HWM != 0) {
          sent += sublist.size();
          if (sent > BACKLOG_HWM) {
            sent = 0;
            throttle(throttleInProgress, endpoints, BACKLOG_HWM);
          }
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

    for (ClientHarness<?> client : clients) {
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
