package com.obsidiandynamics.indigo.iot.client;

import java.nio.*;

public interface SessionListener {
  void onConnect(Session session);
  
  void onDisconnect(Session session);
  
  void onText(String payload);
  
  void onBinary(ByteBuffer payload);
}
