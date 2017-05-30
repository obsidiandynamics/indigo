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
  }
  
  private final EdgeNode node;
  
  private final EdgeRigConfig config;
  
  private final List<Topic> leafTopics;
  
  private final int expectedSubscribers;
  
  private final Gson subframeGson = new Gson();
  
  private volatile boolean running = true;

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
    while (running) {
      try {
        Thread.sleep(config.pulseIntervalMillis);
      } catch (InterruptedException e) {
        continue;
      }
      
      //node.publish(topic, payload);
    }
    
    try {
      node.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public void close() {
    running = false;
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
      runBenchmark(); 
    }
  }
  
  private void runBenchmark() {
    log("e: starting benchmark\n");
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
