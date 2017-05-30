package com.obsidiandynamics.indigo.iot.remote;

import java.nio.*;

public class RemoteNexusHandlerAdapter implements RemoteNexusHandler {
  @Override
  public void onConnect(RemoteNexus nexus) {}
  
  @Override
  public void onDisconnect(RemoteNexus nexus) {}
  
  @Override
  public void onText(RemoteNexus nexus, String topic, String payload) {}
  
  @Override
  public void onBinary(RemoteNexus nexus, String topic, ByteBuffer payload) {}
}
