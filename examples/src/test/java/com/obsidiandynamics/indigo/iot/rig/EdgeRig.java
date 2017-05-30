package com.obsidiandynamics.indigo.iot.rig;

import java.util.*;

import com.google.gson.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class EdgeRig extends Thread implements TestSupport, AutoCloseable, TopicListener {
  public static class EdgeRigConfig {
    TopicGen topicGen;
    int pulseIntervalMillis;
    int pulses;
  }
  
  private enum State {
    CONNECT_WAIT, RUNNING, CLOSING, CLOSED
  }
  
  private final EdgeNode node;
  
  private final EdgeRigConfig config;
  
  private final List<Topic> leafTopics;
  
  private final int expectedSubscribers;
  
  private final Gson subframeGson = new Gson();
  
  private volatile State state = State.CONNECT_WAIT;

  public EdgeRig(EdgeNode node, EdgeRigConfig config) {
    super("EdgeRig");
    this.node = node;
    this.config = config;
    
    leafTopics = config.topicGen.getLeafTopics();
    expectedSubscribers = config.topicGen.getAllInterests().size();
    node.addTopicListener(this);
    start();
  }
  
  @Override
  public void run() {
    while (state != State.CLOSING && state != State.CLOSED) {
      runBenchmark();
    }
    
    try {
      node.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    state = State.CLOSED;
  }
  
  private void runBenchmark() {
    if (state == State.RUNNING) {
      log("e: starting benchmark\n");
    } else {
      return;
    }
    
    int pulse = 0;
    while (state == State.RUNNING) {
      log("e: sending pulse %,d\n", pulse);
      for (Topic t : leafTopics) {
        node.publish(t.toString(), String.valueOf(System.nanoTime()));
      }
      
      if (++pulse < config.pulses) {
        try {
          Thread.sleep(config.pulseIntervalMillis);
        } catch (InterruptedException e) {
          continue;
        }
      } else {
        state = State.CLOSING;
        break;
      }
    }
  }
  
  public void await() throws InterruptedException {
    Await.await(Integer.MAX_VALUE, 10, () -> state == State.CLOSED);
  }
  
  @Override
  public void close() {
    state = State.CLOSING;
    interrupt();
    try {
      join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
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
    log("e: pub %s %s\n", nexus, pub);
    if (pub.getTopic().startsWith(RigSubframe.TOPIC_PREFIX)) {
      final Topic t = Topic.of(pub.getTopic());
      final String remoteId = t.getParts()[1];
      final RigSubframe subframe = RigSubframe.unmarshal(pub.getPayload(), subframeGson);
      onSubframe(remoteId, subframe);
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
