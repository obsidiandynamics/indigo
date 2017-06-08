package com.obsidiandynamics.indigo.iot.edge;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicListener {
  void onConnect(EdgeNexus nexus);
  
  void onDisconnect(EdgeNexus nexus);
  
  void onBind(EdgeNexus nexus, BindFrame bind, BindResponseFrame bindRes);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
