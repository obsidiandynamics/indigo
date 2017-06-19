package com.obsidiandynamics.indigo.iot.remote;

import java.nio.*;

public class RemoteNexusHandlerBase implements RemoteNexusHandler {
  @Override
  public void onOpen(RemoteNexus nexus) {}
  
  @Override
  public void onClose(RemoteNexus nexus) {}
  
  @Override
  public void onText(RemoteNexus nexus, String topic, String payload) {}
  
  @Override
  public void onBinary(RemoteNexus nexus, String topic, ByteBuffer payload) {}
}
