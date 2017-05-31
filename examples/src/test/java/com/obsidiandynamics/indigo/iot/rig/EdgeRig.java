package com.obsidiandynamics.indigo.iot.rig;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.google.gson.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class EdgeRig extends Thread implements TestSupport, AutoCloseable, TopicListener {
  public static class EdgeRigConfig {
    TopicGen topicGen;
    int pulseDurationMillis;
    int pulses;
  }
  
  private enum State {
    CONNECT_WAIT, RUNNING, STOPPED, CLOSING, CLOSED
  }
  
  private final EdgeNode node;
  
  private final EdgeRigConfig config;
  
  private final List<Topic> leafTopics;
  
  private final Gson subframeGson = new Gson();
  
  private volatile State state = State.CONNECT_WAIT;
  
  private final AtomicLong sent = new AtomicLong();

  public EdgeRig(EdgeNode node, EdgeRigConfig config) {
    super("EdgeRig");
    this.node = node;
    this.config = config;
    
    leafTopics = config.topicGen.getLeafTopics();
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
      log("e: starting benchmark\n");
    } else {
      return;
    }

    int perInterval = Math.max(1, leafTopics.size() / config.pulseDurationMillis);
    int interval = 1;
    
    int pulse = 0;
    outer: while (state == State.RUNNING) {
      final long start = System.nanoTime();
      int sent = 0;
      for (Topic t : leafTopics) {
        node.publish(t.toString(), String.valueOf(System.nanoTime()));
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
      log("e: pulse %,d took %,d (%,d every %,d ms)\n", pulse, took, perInterval, interval);
      
      if (++pulse == config.pulses) {
        break;
      }
    }
    state = State.STOPPED;
  }
  
  public void await() throws InterruptedException {
    Await.await(Integer.MAX_VALUE, 10, () -> state == State.STOPPED);
  }
  
  public long getNumSent() {
    return sent.get();
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
    log("e: connect %s\n", nexus);
  }

  @Override
  public void onDisconnect(EdgeNexus nexus) {
    log("e: disconnect %s\n", nexus);
  }

  @Override
  public void onSubscribe(EdgeNexus nexus, SubscribeFrame sub, SubscribeResponseFrame subRes) {
    log("e: sub %s %s\n", nexus, sub);
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
//    log("e: pub %s %s\n", nexus, pub);
    if (pub.getTopic().startsWith(RigSubframe.TOPIC_PREFIX)) {
      final Topic t = Topic.of(pub.getTopic());
      final String remoteId = t.getParts()[1];
      final RigSubframe subframe = RigSubframe.unmarshal(pub.getPayload(), subframeGson);
      onSubframe(remoteId, subframe);
    } else {
      sent.incrementAndGet();
    }
  }
  
  private void onSubframe(String remoteId, RigSubframe subframe) {
    log("e: subframe %s %s\n", remoteId, subframe);
    if (subframe instanceof Sync) {
      sendSubframe(remoteId, new Sync(System.nanoTime()));
    } else if (subframe instanceof Begin) {
      state = State.RUNNING;
    }
  }
  
  private void sendSubframe(String remoteId, RigSubframe subframe) {
    final String topic = RigSubframe.TOPIC_PREFIX + "/" + remoteId + "/in";
    node.publish(topic, subframe.marshal(subframeGson));
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    log("e: pub %s %s\n", nexus, pub);
  }
}
