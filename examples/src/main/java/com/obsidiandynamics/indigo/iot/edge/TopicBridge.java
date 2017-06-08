package com.obsidiandynamics.indigo.iot.edge;

import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicBridge extends AutoCloseable {
  void onConnect(EdgeNexus nexus);
  
  void onDisconnect(EdgeNexus nexus);
  
  CompletableFuture<BindResponseFrame> onBind(EdgeNexus nexus, BindFrame bind);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
