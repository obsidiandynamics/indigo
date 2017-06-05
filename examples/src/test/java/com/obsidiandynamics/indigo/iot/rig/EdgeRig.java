package com.obsidiandynamics.indigo.iot.rig;

import static com.obsidiandynamics.indigo.util.SocketTestSupport.*;

import java.nio.*;
import java.util.*;

import com.google.gson.*;
import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class EdgeRig extends Thread implements TestSupport, AutoCloseable, TopicListener {
  public static class EdgeRigConfig {
    TopicSpec topicSpec;
    int pulseDurationMillis;
    int pulses;
    int warmupPulses;
    boolean text;
    int bytes;
    LogConfig log;
  }
  
  private enum State {
    CONNECT_WAIT, RUNNING, STOPPED, CLOSING, CLOSED
  }
  
  private final EdgeNode node;
  
  private final EdgeRigConfig config;
  
  private final List<Topic> leafTopics;
  
  private final Gson subframeGson = new Gson();
  
  private volatile State state = State.CONNECT_WAIT;
  
  public EdgeRig(EdgeNode node, EdgeRigConfig config) {
    super("EdgeRig");
    this.node = node;
    this.config = config;
    
    leafTopics = config.topicSpec.getLeafTopics();
    node.addTopicListener(this);
    start();
  }
  
  @Override
  public void run() {
    while (state != State.CLOSING) {
      runBenchmark();
    }
  }
  
  private void runBenchmark() {
    if (state == State.RUNNING) {
      if (config.log.verbose) config.log.out.format("e: starting benchmark\n");
    } else {
      return;
    }

    int perInterval = Math.max(1, leafTopics.size() / config.pulseDurationMillis);
    int interval = 1;
    
    int pulse = 0;
    if (config.log.stages) config.log.out.format("Warming up (%,d pulses)...\n", config.warmupPulses);
    boolean warmup = true;
    final byte[] binPayload = config.text ? null : randomBytes(config.bytes);
    final String textPayload = config.text ? randomString(config.bytes) : null;
    final int progressInterval = Math.max(1, config.pulses / 25);
    
    outer: while (state == State.RUNNING) {
      final long start = System.nanoTime();
      int sent = 0;
      for (Topic t : leafTopics) {
        if (warmup && pulse >= config.warmupPulses) {
          warmup = false;
          if (config.log.stages) config.log.out.format("Starting timed run (%,d pulses)...\n", 
                                                       config.pulses - config.warmupPulses);
        }
        final long timestamp = warmup ? 0 : System.nanoTime();
        if (config.text) {
          final String str = new StringBuilder().append(timestamp).append(' ').append(textPayload).toString();
          node.publish(t.toString(), str);
        } else {
          final ByteBuffer buf = ByteBuffer.allocate(8 + config.bytes);
          buf.putLong(timestamp);
          buf.put(binPayload);
          buf.flip();
          node.publish(t.toString(), buf);
        }
        
        if (sent++ % perInterval == 0) {
          try {
            Thread.sleep(interval);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            continue outer;
          }
        }
      }
      final long took = System.nanoTime() - start;
      if (took > config.pulseDurationMillis * 1_000_000l) {
        if (interval > 1) {
          interval--;
        } else {
          perInterval++;
        }
      } else {
        if (perInterval > 1) {
          perInterval--;
        } else {
          interval++;
        }
      }
      if (config.log.verbose) config.log.out.format("e: pulse %,d took %,d (%,d every %,d ms)\n", 
                                                    pulse, took, perInterval, interval);
      
      if (config.log.progress && pulse % progressInterval == 0) {
        config.log.printProgressBlock();
      }
      
      if (++pulse == config.pulses) {
        break;
      }
    }
    state = State.STOPPED;
  }
  
  public void await() throws InterruptedException {
    Await.await(Integer.MAX_VALUE, 10, () -> state == State.STOPPED);
  }
  
  @Override
  public void close() throws Exception {
    state = State.CLOSING;
    interrupt();
    join();
    
    final List<EdgeNexus> nexuses = node.getNexuses();
    for (EdgeNexus nexus : nexuses) {
      nexus.close();
    }
    for (EdgeNexus nexus : nexuses) {
      nexus.awaitClose(60_000);
    }
    node.close();
    state = State.CLOSED;
  }

  @Override
  public void onConnect(EdgeNexus nexus) {
    if (config.log.verbose) config.log.out.format("e: connect %s\n", nexus);
  }

  @Override
  public void onDisconnect(EdgeNexus nexus) {
    if (config.log.verbose) config.log.out.format("e: disconnect %s\n", nexus);
  }

  @Override
  public void onSubscribe(EdgeNexus nexus, SubscribeFrame sub, SubscribeResponseFrame subRes) {
    if (config.log.verbose) config.log.out.format("e: sub %s %s\n", nexus, sub);
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
    if (! nexus.isLocal() && pub.getTopic().startsWith(RigSubframe.TOPIC_PREFIX)) {
      final Topic t = Topic.of(pub.getTopic());
      final String remoteId = t.getParts()[1];
      final RigSubframe subframe = RigSubframe.unmarshal(pub.getPayload(), subframeGson);
      onSubframe(remoteId, subframe);
    }
  }
  
  private void onSubframe(String remoteId, RigSubframe subframe) {
    if (config.log.verbose) config.log.out.format("e: subframe %s %s\n", remoteId, subframe);
    if (subframe instanceof Sync) {
      sendSubframe(remoteId, new Sync(System.nanoTime()));
    } else if (subframe instanceof Begin) {
      state = State.RUNNING;
    }
  }
  
  private void sendSubframe(String remoteId, RigSubframe subframe) {
    final String topic = RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/rx";
    node.publish(topic, subframe.marshal(subframeGson));
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    if (config.log.verbose) config.log.out.format("e: pub %s %s\n", nexus, pub);
  }
}
