package com.obsidiandynamics.indigo.iot.edge;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicListener {
  void onConnect(EdgeNexus nexus);
  
  void onDisconnect(EdgeNexus nexus);
  
  void onSubscribe(EdgeNexus nexus, SubscribeFrame sub, SubscribeResponseFrame subRes);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
