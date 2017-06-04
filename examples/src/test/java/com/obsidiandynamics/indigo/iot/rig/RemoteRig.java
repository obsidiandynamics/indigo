package com.obsidiandynamics.indigo.iot.rig;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.awaitility.*;

import com.google.gson.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.topic.TopicGen.*;
import com.obsidiandynamics.indigo.util.*;

import junit.framework.*;

public final class RemoteRig implements TestSupport, AutoCloseable, ThrowingRunnable, RemoteNexusHandler {
  private static final int CONN_CLOSE_TIMEOUT = 10_000;
  
  public static class RemoteRigConfig {
    int syncSubframes = 10;
    URI uri;
    TopicGen topicGen;
    LogConfig log;
  }
  
  private final RemoteNode node;
  
  private final RemoteRigConfig config;
  
  private final Gson subframeGson = new Gson();
  
  private final Summary summary = new Summary();
  
  private long timeDiff;
  
  private volatile long startTime;
  
  private final AtomicLong received = new AtomicLong();
  
  public RemoteRig(RemoteNode node, RemoteRigConfig config) throws Exception {
    this.node = node;
    this.config = config;
  }
  
  @Override
  public void run() throws Exception {
    timeDiff = config.syncSubframes != 0 ? calibrate() : 0;
    connectAll();
    begin();
  }
  
  private void connectAll() throws Exception {
    final List<Interest> allInterests = config.topicGen.getAllInterests();
    
    final List<CompletableFuture<SubscribeResponseFrame>> futures = new ArrayList<>(allInterests.size());
    for (Interest interest : allInterests) {
      for (int i = 0; i < interest.getCount(); i++) {
        final RemoteNexus nexus = node.open(config.uri, this);
        final CompletableFuture<SubscribeResponseFrame> f = 
            nexus.subscribe(new SubscribeFrame(UUID.randomUUID(), new String[]{interest.getTopic().toString()}, null));
        futures.add(f);
      }
    }
    
    for (CompletableFuture<SubscribeResponseFrame> f : futures) {
      f.get();
    }
    if (config.log.verbose) config.log.out.format("r: %,d remotes connected\n", allInterests.size());
  }
  
  private void begin() throws Exception {
    if (config.log.stages) config.log.out.format("Connecting remotes...\n");
    final RemoteNexus control = node.open(config.uri, new RemoteNexusHandlerAdapter());
    control.publish(new PublishTextFrame(getTxTopic(generateRemoteId()), new Begin().marshal(subframeGson))).get();
    control.close();
    control.awaitClose(CONN_CLOSE_TIMEOUT);
    startTime = System.currentTimeMillis();
  }
  
  private String getRxTopic(String remoteId) {
    return RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/rx";
  }
  
  private String getTxTopic(String remoteId) {
    return RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/tx";
  }
  
  private String generateRemoteId() {
    return Long.toHexString(Crypto.machineRandom());
  }
  
  private long calibrate() throws Exception {
    if (config.log.stages) config.log.out.format("Time calibration...\n");
    final String remoteId = generateRemoteId();
    final String inTopic = getRxTopic(remoteId);
    final String outTopic = getTxTopic(remoteId);
    final int discardSyncs = (int) (config.syncSubframes * .25);
    final AtomicBoolean syncComplete = new AtomicBoolean();
    final AtomicInteger syncs = new AtomicInteger();
    final AtomicLong lastRemoteTransmitTime = new AtomicLong();
    final List<Long> timeDeltas = new CopyOnWriteArrayList<>();
    
    final RemoteNexus nexus = node.open(config.uri, new RemoteNexusHandlerAdapter() {
      @Override public void onText(RemoteNexus nexus, String topic, String payload) {
        final long now = System.nanoTime();
        if (config.log.verbose) config.log.out.format("r: sync text %s %s\n", topic, payload);
        final Sync sync = RigSubframe.unmarshal(payload, subframeGson);
        final long timeTaken = now - lastRemoteTransmitTime.get();
        final long timeDelta = now - sync.getNanoTime() - timeTaken / 2;
        if (syncs.getAndIncrement() >= discardSyncs) {
          if (config.log.verbose) config.log.out.format("r: sync round-trip: %,d, delta: %,d\n", timeTaken, timeDelta);
          timeDeltas.add(timeDelta);
        }
        
        if (timeDeltas.size() != config.syncSubframes) {
          lastRemoteTransmitTime.set(System.nanoTime());
          nexus.publish(new PublishTextFrame(outTopic, new Sync(lastRemoteTransmitTime.get()).marshal(subframeGson)));
        } else {
          try {
            nexus.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      
      @Override public void onDisconnect(RemoteNexus nexus) {
        syncComplete.set(true);
      }
    });
    nexus.subscribe(new SubscribeFrame(UUID.randomUUID(), new String[] {inTopic}, null)).get();
    
    lastRemoteTransmitTime.set(System.nanoTime());
    nexus.publish(new PublishTextFrame(outTopic, new Sync(lastRemoteTransmitTime.get()).marshal(subframeGson)));
    Awaitility.await().atMost(10, TimeUnit.SECONDS).untilTrue(syncComplete);
    
    final long timeDiff = timeDeltas.stream().collect(Collectors.averagingLong(l -> l)).longValue();
    if (config.log.stages) config.log.out.format("Calibration complete; time delta: %,d ns (%s ahead)\n", 
                                                 timeDiff, timeDiff >= 0 ? "remote" : "edge");
    return timeDiff;
  }
  
  public void awaitReceival(long expected) throws InterruptedException {
    Await.await(Integer.MAX_VALUE, 10, () -> received.get() >= expected);
    final long took = System.currentTimeMillis() - startTime;
    TestCase.assertEquals(expected, received.get());
    summary.stats.await();
    summary.compute(Arrays.asList(new Elapsed() {
      @Override public long getTotalProcessed() {
        return received.get();
      }
      @Override public long getTimeTaken() {
        return took;
      }
    }));
  }
  
  @Override
  public void close() throws Exception {
    final List<RemoteNexus> nexuses = node.getNexuses();
    for (RemoteNexus nexus : nexuses) {
      nexus.close();
    }
    for (RemoteNexus nexus : nexuses) {
      nexus.awaitClose(60_000);
    }
    node.close();
  }
  
  public Summary getSummary() {
    return summary;
  }

  @Override
  public void onConnect(RemoteNexus nexus) {
    if (config.log.verbose) config.log.out.format("r: connected %s\n", nexus);
  }

  @Override
  public void onDisconnect(RemoteNexus nexus) {
    if (config.log.verbose) config.log.out.format("r: disconnected %s\n", nexus);
  }

  @Override
  public void onText(RemoteNexus nexus, String topic, String payload) {
    final long now = System.nanoTime();
    final int idx = payload.indexOf(' ');
    final long serverNanos = Long.valueOf(payload.substring(0, idx));
    time(now, serverNanos);
  }

  @Override
  public void onBinary(RemoteNexus nexus, String topic, ByteBuffer payload) {
    final long now = System.nanoTime();
    final long serverNanos = payload.getLong();
    time(now, serverNanos);
  }
  
  private void time(long now, long serverNanos) {
    received.incrementAndGet();
    if (serverNanos == 0) return;
    final long clientNanos = serverNanos + timeDiff;
    final long taken = now - clientNanos;
    if (config.log.verbose) config.log.out.format("r: received; latency %,d\n", taken);
    summary.stats.executor.execute(() -> summary.stats.samples.addValue(taken));
  }
}
