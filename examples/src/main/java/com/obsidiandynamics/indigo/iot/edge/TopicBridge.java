package com.obsidiandynamics.indigo.iot.edge;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicBridge {
  void onConnect(EdgeNexus nexus);
  
  void onDisconnect(EdgeNexus nexus);
  
  CompletableFuture<SubscribeResponseFrame> onSubscribe(EdgeNexus nexus, SubscribeFrame sub);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
