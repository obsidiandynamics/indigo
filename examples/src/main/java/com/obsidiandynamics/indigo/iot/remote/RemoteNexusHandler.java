package com.obsidiandynamics.indigo.iot.remote;

import java.nio.*;

public interface RemoteNexusHandler {
  void onConnect(RemoteNexus node);
  
  void onDisconnect(RemoteNexus node);
  
  void onText(RemoteNexus node, String topic, String payload);
  
  void onBinary(RemoteNexus node, String topic, ByteBuffer payload);
}
