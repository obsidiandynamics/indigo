package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicBridge extends AutoCloseable {
  void onConnect(EdgeNexus nexus);
  
  void onDisconnect(EdgeNexus nexus);
  
  CompletableFuture<Void> onBind(EdgeNexus nexus, List<String> subscribe);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
