package com.obsidiandynamics.indigo.iot.edge;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicBridge extends AutoCloseable {
  void onOpen(EdgeNexus nexus);
  
  void onClose(EdgeNexus nexus);
  
  CompletableFuture<Void> onBind(EdgeNexus nexus, Set<String> subscribe, Set<String> unsubscribe);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
