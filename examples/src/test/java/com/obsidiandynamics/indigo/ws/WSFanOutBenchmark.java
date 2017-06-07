package com.obsidiandynamics.indigo.ws;

import static com.obsidiandynamics.indigo.util.SocketTestSupport.*;
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

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.fake.*;
import com.obsidiandynamics.indigo.ws.jetty.*;
import com.obsidiandynamics.indigo.ws.netty.*;
import com.obsidiandynamics.indigo.ws.undertow.*;

public final class WSFanOutBenchmark implements TestSupport, SocketTestSupport {
  private static final int PORT = 6667;
  private static final int BACKLOG_HWM = 1_000_000;
  private static final int BYTES = 16;
  private static final int IDLE_TIMEOUT = 0;
  
  abstract static class Config implements Spec {
    ServerHarnessFactory serverHarnessFactory;
    ClientHarnessFactory clientHarnessFactory;
    ThrowingRunnable cleanup;
    int port;
    int idleTimeout;
    int n;               // number of outgoing messages per connection
    int m;               // number of connections
    int bytes;           // bytes per message
    boolean echo;        // whether the client should respond to a broadcast
    boolean flush;       // flush on the server after enqueuing (if 'nodelay' is disabled)
    boolean text;        // send text frames instead of binary
    int backlogHwm;
    float warmupFrac;
    LogConfig log;
    int rxtxInterval;
    
    /* Derived fields. */
    int warmupMessages;
    
    @Override
    public void init() {
      warmupMessages = (int) (n * warmupFrac);
    }

    @Override
    public LogConfig getLog() {
      return log;
    }

    @Override
    public String describe() {
      return String.format("%,d messages, %,d connections, %,d bytes/msg, %.0f%% warmup fraction",
                           n, m, bytes, warmupFrac * 100);
    }
    
    SpecMultiplier assignDefaults() {
      port = PORT;
      idleTimeout = IDLE_TIMEOUT;
      n = 100;
      m = 10;
      bytes = BYTES;
      flush = false;
      backlogHwm = BACKLOG_HWM;
      warmupFrac = 0.10f;
      log = new LogConfig() {{
        summary = stages = LOG;
      }};
      rxtxInterval = 5_000;
      return times(1);
    }

    @Override
    public Summary run() throws Exception {
      return new WSFanOutBenchmark().test(this);
    }
  }
  
  private static int totalConnected(List<? extends ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.connected.get() ? 1 : 0).sum();
  }
  
  private static int totalClosed(List<? extends ClientHarness> clients) {
    return clients.stream().mapToInt(c -> c.closed.get() ? 1 : 0).sum();
  }
  
  private static long totalReceived(List<? extends ClientHarness> clients) {
    return clients.stream().mapToLong(c -> c.received.get()).sum();
  }
  
  private static long totalSent(List<? extends ClientHarness> clients) {
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
  
  @SuppressWarnings("unchecked")
  private static <T> T unsafeCast(Object obj) {
    return (T) obj;
  }
  
  private static int getUtBufferSize(int bytes) {
    return Math.max(1024, bytes);
  }
  
  private static ServerHarnessFactory serverHarnessFactory(WSServerFactory<? extends WSEndpoint> serverFactory) throws Exception {
    return (port_, progress, idleTimeout) -> new DefaultServerHarness(new WSServerConfig() {{
      port = port_;
      contextPath = "/";
      idleTimeoutMillis = idleTimeout;
    }}, unsafeCast(serverFactory), progress);
  }
  
  private static ClientHarnessFactory clientHarnessFactory(WSClient<?> client) {
    return (port, echo) -> new DefaultClientHarness(client, port, echo);
  }
  
  private static WSClient<?> createClient(WSClientFactory<? extends WSEndpoint> clientFactory, int idleTimeout) throws Exception {
    return unsafeCast(clientFactory.create(new WSClientConfig() {{
      idleTimeoutMillis = idleTimeout;
    }}));
  }

  private static ClientHarnessFactory fakeClientFactory(int bytes) {
    return (port, echo) -> new FakeClientHarness(port, bytes);
  }
  
  @Test
  public void testNtUt() throws Exception {
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(),
                                                                   getUtBufferSize(BYTES)), IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(NettyServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = false;
      text = false;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testUtUt_noEcho_binary() throws Exception {
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(), getUtBufferSize(BYTES)),
                                            IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = false;
      text = false;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testUtUt_echo_binary() throws Exception {
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(), getUtBufferSize(BYTES)),
                                            IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = true;
      text = false;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testUtUt_noEcho_text() throws Exception {
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(), getUtBufferSize(BYTES)),
                                            IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = false;
      text = true;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testUtUt_echo_text() throws Exception {
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(), getUtBufferSize(BYTES)),
                                            IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = true;
      text = true;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testUtFc() throws Exception {
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = fakeClientFactory(BYTES);
      cleanup = ThrowingRunnable::noOp;
      echo = false;
      text = false;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testJtJt() throws Exception {
    final WSClient<?> client = createClient(JettyClient.factory(createHttpClient()), IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(JettyServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = false;
      text = false;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testUtJt() throws Exception {
    final WSClient<?> client = createClient(JettyClient.factory(createHttpClient()),  IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = false;
      text = false;
    }}.assignDefaults().test();
  }
  
  @Test
  public void testJtUt() throws Exception {
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(), getUtBufferSize(BYTES)), 
                                            IDLE_TIMEOUT);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(JettyServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      echo = false;
      text = false;
    }}.assignDefaults().test();
  }
  
  private void throttle(Config c, AtomicBoolean throttleInProgress, List<? extends WSEndpoint> endpoints, int backlogHwm) {
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
        if (c.log.stages && ! logged) {
          c.log.out.format("s: throttling", minTotalBacklog);
          logged = true;
        }
        if (logged && ++waits % 1000 == 0) {
          c.log.out.format(".");
        }
        TestSupport.sleep(1);
      } else if (minTotalBacklog < backlogHwm / 2) {
        if (logged) {
          c.log.out.format("\n");
          throttleInProgress.set(false);
        }
        break;
      }
    }
  }
  
  private Summary test(Config c) throws Exception {
    final List<ClientHarness> clients = new ArrayList<>(c.m);
    final AtomicBoolean throttleInProgress = new AtomicBoolean();
    
    final ServerProgress progress = new ServerProgress() {
      private final AtomicBoolean updateInProgress = new AtomicBoolean();
      private long firstUpdate;
      private long lastUpdate;
      private long lastSent;
      private long lastReceived;
      @Override public void update(ServerHarness server, long sent) {
        if (c.rxtxInterval == 0) return;
        
        final long now = System.currentTimeMillis();
        if (sent == 0) {
          firstUpdate = lastUpdate = now;
          return;
        }
        
        final long timeDelta = now - lastUpdate;
        if (timeDelta < c.rxtxInterval) return;
        
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
              c.log.out.format("> tx: %,d, cur: %,.0f/s, avg: %,.0f/s\n", sent, txCurrentRate, txAverageRate);
              c.log.out.format("< rx: %,d, cur: %,.0f/s, avg: %,.0f/s\n", received, rxCurrentRate, rxAverageRate);
            }
          } finally {
            updateInProgress.set(false);
          }
        }
      }
    };
    
    final ServerHarness server = c.serverHarnessFactory.create(c.port, progress, c.idleTimeout);
    try {
      return test(c, throttleInProgress, clients, server);
    } finally {
      server.close();
      c.cleanup.run();
    }
  }
  
  private Summary test(Config c,
                       AtomicBoolean throttleInProgress,
                       List<ClientHarness> clients,
                       ServerHarness server) throws Exception {
    final int sendThreads = 1;
    final int waitScale = 1 + (int) (((long) c.n * (long) c.m) / 1_000_000_000l);
    
    for (int i = 0; i < c.m; i++) {
      clients.add(c.clientHarnessFactory.create(c.port, c.echo)); 
    }

    if (c.log.stages) c.log.out.format("s: awaiting server.connected\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.connected.get() == c.m);

    assertEquals(c.m, server.connected.get());
    assertEquals(c.m, totalConnected(clients));

    final byte[] binPayload = c.text ? null : randomBytes(c.bytes);
    final String textPayload = c.text ? randomString(c.bytes) : null;
    
    final List<WSEndpoint> endpoints = server.getEndpoints();

    final int progressInterval = Math.max(1, c.n / 25);
    if (c.log.stages) c.log.out.format("s: warming up...\n");
    for (int i = 0; i < c.warmupMessages; i++) {
      long sent = 0;
      if (c.text) {
        server.broadcast(endpoints, textPayload);
      } else {
        server.broadcast(endpoints, binPayload);
      }
      
      if (c.backlogHwm != 0) {
        sent += endpoints.size();
        if (sent > c.backlogHwm) {
          sent = 0;
          throttle(c, throttleInProgress, endpoints, c.backlogHwm);
        }
      }
      if (c.log.progress && i % progressInterval == 0) c.log.printProgressBlock();
    }

    final long timedRuns = c.n - c.warmupMessages;
    final long start = System.currentTimeMillis();
    if (c.log.stages) c.log.out.format("s: starting timed run...\n");
    ParallelJob.blockingSlice(endpoints, sendThreads, sublist -> {
      long sent = 0;
      for (int i = 0; i < timedRuns; i++) {
        if (c.text) {
          server.broadcast(sublist, textPayload);
        } else {
          server.broadcast(sublist, binPayload);
        }
        
        if (c.backlogHwm != 0) {
          sent += sublist.size();
          if (sent > c.backlogHwm) {
            sent = 0;
            throttle(c, throttleInProgress, endpoints, c.backlogHwm);
          }
        }
        if (c.log.progress && (i + c.warmupMessages) % progressInterval == 0) c.log.printProgressBlock();
      }
      
      if (c.flush) {
        if (c.log.stages) c.log.out.format("s: flushing\n");
        for (int i = 0; i < c.n; i++) {
          try {
            server.flush(sublist);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }).run();

    if (c.log.stages) c.log.out.format("s: awaiting server.sent\n");
    final long expectedReceive = (long) c.m * c.n;
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.sent.get() >= expectedReceive);
    assertEquals(expectedReceive, server.sent.get());

    if (c.log.stages) c.log.out.format("s: awaiting client.received\n");
    long waitStart = System.currentTimeMillis();
    long lastPrint = System.currentTimeMillis();
    for (;;) {
      final long totalReceived = totalReceived(clients);
      if (totalReceived >= expectedReceive) {
        break;
      } else if (System.currentTimeMillis() - lastPrint > 1000) {
        final long takingSeconds = (System.currentTimeMillis() - waitStart) / 1000;
        if (c.log.stages) c.log.out.format("s: ... %,d seconds later %,d received\n", takingSeconds, totalReceived);
        lastPrint = System.currentTimeMillis();
      } else {
        Thread.sleep(10);
      }
    }
    
    assertEquals(expectedReceive, totalReceived(clients));

    if (c.log.stages) c.log.out.format("s: awaiting client.sent\n");
    if (c.echo) {
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalSent(clients) >= expectedReceive);
      assertEquals(expectedReceive, totalSent(clients));
    } else {
      assertEquals(0, totalSent(clients));
    }
    
    final Summary summary = new Summary();
    summary.compute(Arrays.asList(new Elapsed() {
      @Override public long getTotalProcessed() {
        return (c.echo ? 2L : 1L) * timedRuns * c.m;
      }
      @Override public long getTimeTaken() {
        return System.currentTimeMillis() - start;
      }
    }));

    for (ClientHarness client : clients) {
      client.close();
    }

    if (c.log.stages) c.log.out.format("s: awaiting server.closed\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.closed.get() == c.m);
    assertEquals(c.m, server.closed.get());

    if (c.log.stages) c.log.out.format("s: awaiting client.closed\n");
    Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> totalClosed(clients) == c.m);
    assertEquals(c.m, totalClosed(clients));

    if (c.echo) {
      if (c.log.stages) c.log.out.format("s: awaiting server.received\n");
      Awaitility.await().atMost(60 * waitScale, TimeUnit.SECONDS).until(() -> server.received.get() == expectedReceive);
      assertEquals(expectedReceive, server.received.get());
    } else {
      assertEquals(0, server.received.get());
    }

    server.close();
    return summary;
  }
  
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    final int bytes = 16;
    final int idleTimeout_ = 0;
    final WSClient<?> client = createClient(UndertowClient.factory(createXnioWorker(), getUtBufferSize(bytes)), 
                                            idleTimeout_);
    new Config() {{
      serverHarnessFactory = serverHarnessFactory(UndertowServer.factory());
      clientHarnessFactory = clientHarnessFactory(client);
      cleanup = client::close;
      port = PORT;
      idleTimeout = idleTimeout_;
      n = 10_000;
      m = 1000;
      bytes = 16;
      echo = false;
      flush = false;
      text = false;
      backlogHwm = BACKLOG_HWM;
      warmupFrac = 0.10f;
      log = new LogConfig() {{
        stages = false;
        progress = intermediateSummaries = true;
        summary = true;
      }};;
      rxtxInterval = 0;
    }}.testPercentile(1, 5, 50, Summary::byThroughput);
  }
}
