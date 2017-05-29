package com.obsidiandynamics.indigo.iot.rig;

import java.util.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class EdgeRig extends Thread implements TestSupport, AutoCloseable, TopicListener {
  public static class EdgeRigConfig {
    int pulseIntervalMillis;
  }
  
  private final EdgeNode node;
  
  private final TopicGen topicGen;
  
  private final EdgeRigConfig config;
  
  private final List<Topic> leafTopics;
  
  private final int expectedSubscribers;
  
  private volatile boolean running = true;

  public EdgeRig(EdgeNode node, TopicGen topicGen, EdgeRigConfig config) {
    super("EdgeRig");
    this.node = node;
    this.topicGen = topicGen;
    this.config = config;
    
    leafTopics = topicGen.getLeafTopics();
    expectedSubscribers = topicGen.getAllInterests().size();
    node.addTopicListener(this);
  }
  
  @Override
  public void run() {
    while (running) {
      try {
        Thread.sleep(config.pulseIntervalMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
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
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onDisconnect(EdgeNexus nexus) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onSubscribe(EdgeNexus nexus, SubscribeFrame sub, SubscribeResponseFrame subRes) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishTextFrame pub) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onPublish(EdgeNexus nexus, PublishBinaryFrame pub) {
    // TODO Auto-generated method stub
    
  }
}
