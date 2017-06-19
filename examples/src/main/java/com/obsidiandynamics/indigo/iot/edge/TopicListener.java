package com.obsidiandynamics.indigo.iot.edge;

import com.obsidiandynamics.indigo.iot.frame.*;

public interface TopicListener {
  void onOpen(EdgeNexus nexus);
  
  void onClose(EdgeNexus nexus);
  
  void onBind(EdgeNexus nexus, BindFrame bind, BindResponseFrame bindRes);
  
  void onPublish(EdgeNexus nexus, PublishTextFrame pub);
  
  void onPublish(EdgeNexus nexus, PublishBinaryFrame pub);
}
