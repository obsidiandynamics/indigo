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
    timeDiff = config.syncSubframes != 0 ? sync() : 0;
    connectAll();
    begin();
  }
  
  private void connectAll() throws Exception {
    final List<Interest> allInterests = config.topicGen.getAllInterests();
    
    final List<CompletableFuture<SubscribeResponseFrame>> futures = new ArrayList<>(allInterests.size());
    for (Interest interest : allInterests) {
      final RemoteNexus nexus = node.open(config.uri, this);
      final CompletableFuture<SubscribeResponseFrame> f = 
          nexus.subscribe(new SubscribeFrame(UUID.randomUUID(), new String[]{interest.getTopic().toString()}, null));
      futures.add(f);
    }
    
    for (CompletableFuture<SubscribeResponseFrame> f : futures) {
      f.get();
    }
    log("r: %,d remotes connected\n", allInterests.size());
  }
  
  private void begin() throws Exception {
    log("r: sending begin command\n");
    final RemoteNexus control = node.open(config.uri, new RemoteNexusHandlerAdapter());
    control.publish(new PublishTextFrame(getOutTopic(generateRemoteId()), new Begin().marshal(subframeGson))).get();
    control.close();
    control.awaitClose(CONN_CLOSE_TIMEOUT);
    startTime = System.currentTimeMillis();
  }
  
  private String getInTopic(String remoteId) {
    return RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/in";
  }
  
  private String getOutTopic(String remoteId) {
    return RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/out";
  }
  
  private String generateRemoteId() {
    return Long.toHexString(Crypto.machineRandom());
  }
  
  private long sync() throws Exception {
    log("r: syncing\n");
    final String remoteId = generateRemoteId();
    final String inTopic = getInTopic(remoteId);
    final String outTopic = getOutTopic(remoteId);
    final int discardSyncs = (int) (config.syncSubframes * .25);
    final AtomicBoolean syncComplete = new AtomicBoolean();
    final AtomicInteger syncs = new AtomicInteger();
    final AtomicLong lastRemoteTransmitTime = new AtomicLong();
    final List<Long> timeDeltas = new CopyOnWriteArrayList<>();
    
    final RemoteNexus nexus = node.open(config.uri, new RemoteNexusHandlerAdapter() {
      @Override public void onText(RemoteNexus nexus, String topic, String payload) {
        final long now = System.nanoTime();
        log("r: sync text %s %s\n", topic, payload);
        final Sync sync = RigSubframe.unmarshal(payload, subframeGson);
        final long timeTaken = now - lastRemoteTransmitTime.get();
        final long timeDelta = now - sync.getNanoTime() - timeTaken / 2;
        if (syncs.getAndIncrement() >= discardSyncs) {
          log("r: sync round-trip: %,d, delta: %,d\n", timeTaken, timeDelta);
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
    log("r: sync complete, diff: %,d\n", timeDiff);
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
    node.close();
  }
  
  public Summary getSummary() {
    return summary;
  }

  @Override
  public void onConnect(RemoteNexus nexus) {
    log("r: connected %s\n", nexus);
  }

  @Override
  public void onDisconnect(RemoteNexus nexus) {
    log("r: disconnected %s\n", nexus);
  }

  @Override
  public void onText(RemoteNexus nexus, String topic, String payload) {
    final long now = System.nanoTime();
    final long serverNanos = Long.valueOf(payload);
    final long clientNanos = serverNanos + timeDiff;
    final long taken = now - clientNanos;
    log("r: received text, latency %,d\n", taken);
    summary.stats.executor.execute(() -> summary.stats.samples.addValue(taken));
    received.incrementAndGet();
  }

  @Override
  public void onBinary(RemoteNexus nexus, String topic, ByteBuffer payload) {
    // TODO Auto-generated method stub
    
  }

//  @Override
//  public void onPublish(RemoteNexus nexus, PublishTextFrame pub) {
//    log("e: pub %s %s\n", nexus, pub);
//    if (pub.getTopic().startsWith(RigSubframe.TOPIC_PREFIX)) {
//      final Topic t = Topic.of(pub.getTopic());
//      final String remoteId = t.getParts()[1];
//      final RigSubframe subframe = RigSubframe.unmarshal(pub.getPayload(), subframeGson);
//      onSubframe(remoteId, subframe);
//    }
//  }
//  
//  private void onSubframe(RemoteNexus nexus, String remoteId, RigSubframe subframe) {
//    log("e: subframe %s %s\n", remoteId, subframe);
//    if (subframe instanceof Sync) {
//      sendSubframe(remoteId, new Sync(System.nanoTime()));
//    }
//  }
//  
//  private void sendSubframe(RemoteNexus nexus, String remoteId, RigSubframe subframe) {
//    final String topic = RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/in";
//    node.publish(topic, subframe.marshal(subframeGson));
//  }

}
